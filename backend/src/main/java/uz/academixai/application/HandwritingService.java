package uz.academixai.application;

import com.pgvector.PGvector;
import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.academixai.domain.HandwritingCheckResult;
import uz.academixai.domain.HandwritingProfile;
import uz.academixai.domain.HandwritingResetLog;
import uz.academixai.domain.PlagiarismType;
import uz.academixai.domain.ResetReason;
import uz.academixai.infrastructure.ai.DocumentTextLayout;
import uz.academixai.infrastructure.ai.HandwritingFeatureExtractor;
import uz.academixai.infrastructure.persistence.HandwritingProfileEntity;
import uz.academixai.infrastructure.persistence.HandwritingProfileRepository;
import uz.academixai.infrastructure.persistence.HandwritingResetLogEntity;
import uz.academixai.infrastructure.persistence.HandwritingResetLogRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_backend_tdd.md §6.2/§6.3 — handwriting fingerprint check/update/reset, implementing that
 * section's pseudocode exactly (100% on first sample, running average for the first 5 samples, then
 * 0.1-alpha interpolation once reliable, 70%/60% thresholds, versioned reset with a 3-per-quarter
 * limit).
 *
 * <p><b>Architecture deviation from TZ §4's literal {@code checkHandwriting(studentId, imageUrl)}
 * signature (judgment call, see ROADMAP.md Sprint 5):</b> this takes an already-parsed {@link
 * DocumentTextLayout} instead of re-downloading the image and calling Vision a second time — {@code
 * AIAnalysisService} already gets the layout from the same Vision call it makes for OCR text (task
 * 48). Same cost-optimization instinct as TZ §3.2's combined grading+plagiarism Qwen call.
 *
 * <p><b>pgvector via native queries, not JPA:</b> {@code feature_vector}'s Postgres {@code vector}
 * type has no Hibernate mapping in this stack — {@link HandwritingProfileEntity} deliberately
 * excludes that column, and this service reads/writes it directly via {@link EntityManager} native
 * queries using {@code pgvector-java}'s {@link PGvector} for the text conversion (matches the
 * project's existing precedent of raw native queries for Postgres-specific behavior JPA can't
 * express, e.g. {@code RlsTransactionFilter}'s {@code SET LOCAL}). Cosine similarity itself is
 * computed in Java, not via pgvector's {@code <=>} operator — both vectors are already in memory
 * for the blending/averaging math below, and this is a direct 1:1 comparison against one known
 * student, not a nearest-neighbor search (the {@code ivfflat} index exists for a possible future
 * anomaly-detection use, not required here).
 */
@Service
public class HandwritingService {

  private static final int RELIABLE_AFTER_SAMPLES = 5;
  private static final float RELIABLE_UPDATE_THRESHOLD = 70.0f;
  private static final float MISMATCH_THRESHOLD = 60.0f;
  private static final float INTERPOLATION_ALPHA = 0.1f;
  private static final int MAX_RESETS_PER_QUARTER = 3;

  private final HandwritingProfileRepository profileRepository;
  private final HandwritingResetLogRepository resetLogRepository;
  private final HandwritingFeatureExtractor featureExtractor;
  private final EntityManager entityManager;
  private final StudentProfileRepository studentProfileRepository;
  private final SchoolClassRepository classRepository;

  public HandwritingService(
      HandwritingProfileRepository profileRepository,
      HandwritingResetLogRepository resetLogRepository,
      HandwritingFeatureExtractor featureExtractor,
      EntityManager entityManager,
      StudentProfileRepository studentProfileRepository,
      SchoolClassRepository classRepository) {
    this.profileRepository = profileRepository;
    this.resetLogRepository = resetLogRepository;
    this.featureExtractor = featureExtractor;
    this.entityManager = entityManager;
    this.studentProfileRepository = studentProfileRepository;
    this.classRepository = classRepository;
  }

  // Required: writeFeatureVector/clearFeatureVector run a raw EntityManager native UPDATE, which
  // needs an active transaction — real, not just a test-harness artifact. This class's two real
  // callers (AIAnalysisService via the queue consumer's TransactionTemplate, and the teacher/
  // admin HTTP endpoints via RlsTransactionFilter) already run inside one, but relying on every
  // future caller to remember that implicitly is fragile; @Transactional makes it explicit and
  // self-sufficient (confirmed by a real TransactionRequiredException from a Testcontainers test
  // that called this service directly, with no surrounding transaction).
  @Transactional
  public HandwritingCheckResult checkAndUpdateProfile(UUID studentId, DocumentTextLayout layout) {
    float[] currentFeatures = featureExtractor.extract(layout);
    Optional<HandwritingProfileEntity> existing = profileRepository.findByStudentId(studentId);
    float[] dbVector = existing.isEmpty() ? null : readFeatureVector(studentId);

    if (existing.isEmpty() || dbVector == null) {
      HandwritingProfile created =
          new HandwritingProfile(
              existing.map(HandwritingProfileEntity::getId).orElse(UUID.randomUUID()),
              studentId,
              1,
              false,
              null,
              existing.map(HandwritingProfileEntity::getProfileVersion).orElse("v1"),
              LocalDateTime.now(),
              null,
              null,
              null,
              existing.map(HandwritingProfileEntity::getResetCountThisQuarter).orElse(0));
      profileRepository.save(HandwritingProfileEntity.fromDomain(created));
      entityManager.flush();
      writeFeatureVector(studentId, currentFeatures);
      return new HandwritingCheckResult(100.0f, false, PlagiarismType.CLEAN);
    }

    HandwritingProfileEntity dbEntity = existing.get();
    int samplesCount = dbEntity.getSamplesCount();
    boolean wasReliable = dbEntity.isReliable();

    float similarity = cosineSimilarity(currentFeatures, dbVector);
    float matchScore = ((similarity + 1.0f) / 2.0f) * 100.0f;

    if (wasReliable && matchScore >= RELIABLE_UPDATE_THRESHOLD) {
      float[] updated = interpolate(dbVector, currentFeatures, INTERPOLATION_ALPHA);
      saveProfileUpdate(dbEntity, samplesCount + 1, true, updated);
    } else if (samplesCount < RELIABLE_AFTER_SAMPLES) {
      float[] updated = average(dbVector, currentFeatures, samplesCount);
      boolean newlyReliable = (samplesCount + 1) >= RELIABLE_AFTER_SAMPLES;
      saveProfileUpdate(dbEntity, samplesCount + 1, newlyReliable, updated);
    }
    // else: reliable profile, low-scoring sample — deliberately not blended in, so a possibly
    // fraudulent submission can't corrupt an established fingerprint.

    PlagiarismType type =
        (wasReliable && matchScore < MISMATCH_THRESHOLD)
            ? PlagiarismType.HANDWRITING_MISMATCH
            : PlagiarismType.CLEAN;
    return new HandwritingCheckResult(matchScore, wasReliable, type);
  }

  /**
   * backend_tdd.md §6.2 {@code resetHandwritingProfile} — teacher-direct, versioned, audited.
   *
   * <p>{@code noRollbackFor = ApiException.class}: this method participates in (joins, doesn't
   * start) the outer HTTP-request transaction {@code RlsTransactionFilter} opens. Without this, the
   * {@code ERR_RESET_LIMIT_EXCEEDED}/{@code ERR_ACCESS_DENIED} guard throws below would mark that
   * *shared* transaction rollback-only — {@code GlobalExceptionHandler} still builds and flushes a
   * clean 403 response, but the outer transaction then fails to commit with {@code
   * UnexpectedRollbackException} *after* the response body is already committed, corrupting the
   * response for real HTTP clients (confirmed via a real browser test: curl got a clean 403,
   * Chrome's XHR saw an aborted/opaque response). Safe here specifically because both guards throw
   * before any {@code save()}/write call in this method — there is nothing to accidentally commit.
   */
  @Transactional(noRollbackFor = ApiException.class)
  public ResetResult resetProfile(
      UUID schoolId, UUID studentId, UUID teacherId, ResetReason reason, String notes) {
    requireClassTeacher(schoolId, studentId, teacherId);
    Optional<HandwritingProfileEntity> existing = profileRepository.findByStudentId(studentId);
    int currentResetCount =
        existing.map(HandwritingProfileEntity::getResetCountThisQuarter).orElse(0);
    if (currentResetCount >= MAX_RESETS_PER_QUARTER) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_RESET_LIMIT_EXCEEDED",
          "Bu chorakda reset limiti tugagan.",
          "Faqat administrator blokdan chiqarishi mumkin.");
    }

    String previousVersion = existing.map(HandwritingProfileEntity::getProfileVersion).orElse(null);
    HandwritingResetLog log =
        new HandwritingResetLog(
            UUID.randomUUID(),
            schoolId,
            studentId,
            teacherId,
            reason,
            notes,
            previousVersion,
            LocalDateTime.now());
    resetLogRepository.save(HandwritingResetLogEntity.fromDomain(log));

    String newVersion = incrementVersion(previousVersion == null ? "v0" : previousVersion);
    int newResetCount = currentResetCount + 1;
    HandwritingProfile updated =
        new HandwritingProfile(
            existing.map(HandwritingProfileEntity::getId).orElse(UUID.randomUUID()),
            studentId,
            0,
            false,
            null,
            newVersion,
            LocalDateTime.now(),
            teacherId,
            reason,
            LocalDateTime.now(),
            newResetCount);
    profileRepository.save(HandwritingProfileEntity.fromDomain(updated));
    entityManager.flush();
    clearFeatureVector(studentId);

    return new ResetResult(newVersion, newResetCount);
  }

  /**
   * academix_tz.md §2.2 admin unlock — resets the quarter counter after the 3-reset limit hit.
   * {@code noRollbackFor}: same reasoning as {@link #resetProfile} — the {@code ERR_NOT_FOUND}
   * guard throws before any write.
   */
  @Transactional(noRollbackFor = ApiException.class)
  public void unlockReset(UUID studentId) {
    HandwritingProfileEntity entity =
        profileRepository
            .findByStudentId(studentId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_NOT_FOUND",
                        "Yozuv profili topilmadi.",
                        "ID ni tekshiring."));
    HandwritingProfile domain = entity.toDomain();
    HandwritingProfile unlocked =
        new HandwritingProfile(
            domain.id(),
            domain.studentId(),
            domain.samplesCount(),
            domain.isReliable(),
            null,
            domain.profileVersion(),
            domain.lastUpdatedAt(),
            domain.lastResetByTeacherId(),
            domain.lastResetReason(),
            domain.lastResetAt(),
            0);
    profileRepository.save(HandwritingProfileEntity.fromDomain(unlocked));
  }

  public record ResetResult(String newProfileVersion, int resetCountThisQuarter) {}

  /**
   * academix_tz.md §2.2 "faqat sinf rahbari" (only the class/homeroom teacher) — resolved via
   * {@code school_classes.class_teacher_id}, the same column {@code AdminClassController} already
   * writes at class-creation time (Sprint 1). Not the same concept as a subject teacher's {@code
   * class_subject_teachers} link.
   */
  private void requireClassTeacher(UUID schoolId, UUID studentId, UUID teacherId) {
    StudentProfileEntity profile =
        studentProfileRepository
            .findByUserIdAndSchoolId(studentId, schoolId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_NOT_FOUND",
                        "O'quvchi topilmadi.",
                        "ID ni tekshiring."));
    SchoolClassEntity schoolClass =
        classRepository
            .findById(profile.getClassId())
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_NOT_FOUND",
                        "Sinf topilmadi.",
                        "ID ni tekshiring."));
    if (!teacherId.equals(schoolClass.getClassTeacherId())) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Faqat sinf rahbari yozuv profilini reset qila oladi.",
          "Bu o'quvchining sinf rahbari bilan bog'laning.");
    }
  }

  private void saveProfileUpdate(
      HandwritingProfileEntity dbEntity, int samplesCount, boolean isReliable, float[] vector) {
    HandwritingProfile domain = dbEntity.toDomain();
    HandwritingProfile updated =
        new HandwritingProfile(
            domain.id(),
            domain.studentId(),
            samplesCount,
            isReliable,
            null,
            domain.profileVersion(),
            LocalDateTime.now(),
            domain.lastResetByTeacherId(),
            domain.lastResetReason(),
            domain.lastResetAt(),
            domain.resetCountThisQuarter());
    profileRepository.save(HandwritingProfileEntity.fromDomain(updated));
    entityManager.flush();
    writeFeatureVector(domain.studentId(), vector);
  }

  private float[] readFeatureVector(UUID studentId) {
    Object result =
        entityManager
            .createNativeQuery(
                "SELECT feature_vector::text FROM handwriting_profiles WHERE student_id = :studentId")
            .setParameter("studentId", studentId)
            .getSingleResult();
    if (result == null) {
      return null;
    }
    try {
      return new PGvector((String) result).toArray();
    } catch (SQLException e) {
      throw new IllegalStateException("Corrupt feature_vector for student " + studentId, e);
    }
  }

  private void writeFeatureVector(UUID studentId, float[] vector) {
    entityManager
        .createNativeQuery(
            "UPDATE handwriting_profiles SET feature_vector = CAST(:vec AS vector) WHERE student_id = :studentId")
        .setParameter("vec", new PGvector(vector).getValue())
        .setParameter("studentId", studentId)
        .executeUpdate();
  }

  private void clearFeatureVector(UUID studentId) {
    entityManager
        .createNativeQuery(
            "UPDATE handwriting_profiles SET feature_vector = NULL WHERE student_id = :studentId")
        .setParameter("studentId", studentId)
        .executeUpdate();
  }

  static String incrementVersion(String version) {
    int number = Integer.parseInt(version.substring(1));
    return "v" + (number + 1);
  }

  static float cosineSimilarity(float[] a, float[] b) {
    double dot = 0;
    double normA = 0;
    double normB = 0;
    for (int i = 0; i < a.length; i++) {
      dot += a[i] * b[i];
      normA += a[i] * a[i];
      normB += b[i] * b[i];
    }
    if (normA == 0 || normB == 0) {
      return 0;
    }
    return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
  }

  static float[] interpolate(float[] oldVector, float[] newVector, float alpha) {
    float[] result = new float[oldVector.length];
    for (int i = 0; i < oldVector.length; i++) {
      result[i] = oldVector[i] * (1 - alpha) + newVector[i] * alpha;
    }
    return result;
  }

  static float[] average(float[] oldVector, float[] newVector, int priorSamples) {
    float[] result = new float[oldVector.length];
    for (int i = 0; i < oldVector.length; i++) {
      result[i] = (oldVector[i] * priorSamples + newVector[i]) / (priorSamples + 1);
    }
    return result;
  }
}
