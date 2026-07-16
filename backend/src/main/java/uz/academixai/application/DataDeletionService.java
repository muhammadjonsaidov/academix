package uz.academixai.application;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.academixai.domain.DataDeletionRequest;
import uz.academixai.domain.DeletionRequestStatus;
import uz.academixai.infrastructure.persistence.DataDeletionRequestEntity;
import uz.academixai.infrastructure.persistence.DataDeletionRequestRepository;
import uz.academixai.infrastructure.persistence.ParentStudentLinkRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §5.6 / §7.6 — retention for minors' biometric/psychological data. Real pseudocode
 * is given verbatim (backend_tdd.md §7.6), followed exactly.
 *
 * <p><b>Sprint 8 update:</b> {@link #requestDeletion} (backing §2.7's {@code POST
 * /parent/children/{studentId}/data-deletion-request}) is now built — Sprint 7 deferred it because
 * it needs {@code parent_student_links} to verify "is this really your child" before accepting a
 * request, and that table didn't exist yet. It does now.
 */
@Service
public class DataDeletionService {

  private final DataDeletionRequestRepository requestRepository;
  private final ParentStudentLinkRepository parentStudentLinkRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final EntityManager entityManager;
  private static final Logger log = LoggerFactory.getLogger(DataDeletionService.class);

  public DataDeletionService(
      DataDeletionRequestRepository requestRepository,
      ParentStudentLinkRepository parentStudentLinkRepository,
      StudentProfileRepository studentProfileRepository,
      EntityManager entityManager) {
    this.requestRepository = requestRepository;
    this.parentStudentLinkRepository = parentStudentLinkRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.entityManager = entityManager;
  }

  /**
   * academix_tz.md §2.5 — {@code POST /parent/children/{studentId}/data-deletion-request}, "only if
   * student isActive=false" (read as {@code student_profiles.is_active} — a student who has left
   * the school, e.g. graduated/transferred, not a currently-enrolled one). Unblocks Sprint 7's
   * deferred gap now that {@code parent_student_links} exists to verify "is this really your child"
   * before accepting the request.
   */
  public DataDeletionRequest requestDeletion(UUID parentUserId, UUID studentId) {
    boolean isLinked =
        parentStudentLinkRepository.existsByParentUserIdAndStudentUserIdAndIsActiveTrue(
            parentUserId, studentId);
    if (!isLinked) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Bu farzandingiz emas.",
          "Faqat o'zingizga bog'langan farzandlar uchun so'rov yuborishingiz mumkin.");
    }
    StudentProfileEntity profile =
        studentProfileRepository
            .findByUserId(studentId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_STUDENT_NOT_FOUND",
                        "O'quvchi topilmadi.",
                        "ID ni tekshiring."));
    if (profile.isActive()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_STUDENT_STILL_ACTIVE",
          "Faol o'quvchi uchun ma'lumotlarni o'chirish so'rovi yuborib bo'lmaydi.",
          "O'quvchi maktabni tark etgandan so'ng qayta urinib ko'ring.");
    }

    DataDeletionRequest request =
        new DataDeletionRequest(
            UUID.randomUUID(),
            profile.getSchoolId(),
            studentId,
            parentUserId,
            DeletionRequestStatus.PENDING,
            LocalDateTime.now(),
            null);
    return requestRepository.save(DataDeletionRequestEntity.fromDomain(request)).toDomain();
  }

  public List<DataDeletionRequest> listPending(UUID schoolId) {
    return requestRepository
        .findBySchoolIdAndStatusOrderByRequestedAtDesc(schoolId, DeletionRequestStatus.PENDING)
        .stream()
        .map(DataDeletionRequestEntity::toDomain)
        .toList();
  }

  /**
   * backend_tdd.md §7.6's exact pseudocode: nulls handwriting {@code feature_vector}/{@code
   * is_reliable} and psychological_signals' {@code raw_evidence}/{@code description} for the
   * student — rows survive (audit/aggregation), only the sensitive content disappears.
   */
  @Transactional
  public DataDeletionRequest approveDataDeletion(UUID schoolId, UUID requestId) {
    DataDeletionRequestEntity entity =
        requestRepository
            .findByIdAndSchoolId(requestId, schoolId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_NOT_FOUND",
                        "So'rov topilmadi.",
                        "ID ni tekshiring."));
    UUID studentId = entity.getStudentId();

    entityManager
        .createNativeQuery(
            "UPDATE handwriting_profiles SET feature_vector = NULL, is_reliable = FALSE WHERE student_id = :studentId")
        .setParameter("studentId", studentId)
        .executeUpdate();
    entityManager
        .createNativeQuery(
            "UPDATE psychological_signals SET raw_evidence = NULL, description = NULL WHERE student_id = :studentId")
        .setParameter("studentId", studentId)
        .executeUpdate();

    DataDeletionRequest domain = entity.toDomain();
    DataDeletionRequest approved =
        new DataDeletionRequest(
            domain.id(),
            domain.schoolId(),
            domain.studentId(),
            domain.requestedBy(),
            DeletionRequestStatus.APPROVED,
            domain.requestedAt(),
            LocalDateTime.now());
    return requestRepository.save(DataDeletionRequestEntity.fromDomain(approved)).toDomain();
  }

  // Scheduled, oyiga bir marta — resolved signal 2 yildan eski bo'lsa anonimlashtiriladi.
  @Scheduled(cron = "0 0 3 1 * *")
  @Transactional
  public void anonymizeOldResolvedSignals() {
    int updated =
        entityManager
            .createNativeQuery(
                "UPDATE psychological_signals SET description = NULL, raw_evidence = NULL"
                    + " WHERE resolved = TRUE AND resolved_at < NOW() - INTERVAL '2 years'")
            .executeUpdate();
    log.info("Monthly psychological-signal anonymization: {} rows anonymized", updated);
  }
}
