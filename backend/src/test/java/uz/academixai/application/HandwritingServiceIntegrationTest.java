package uz.academixai.application;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;
import uz.academixai.TestcontainersConfiguration;
import uz.academixai.domain.HandwritingCheckResult;
import uz.academixai.domain.PlagiarismType;
import uz.academixai.domain.ResetReason;
import uz.academixai.infrastructure.ai.CharacterBox;
import uz.academixai.infrastructure.ai.DocumentTextLayout;

/**
 * Real Postgres + real pgvector (native-query read/write path, see {@link HandwritingService} class
 * doc) — no mocks, matching this project's "infra-touching code gets a real Testcontainers test"
 * discipline (write-integration-test skill). {@code GoogleVisionClient} isn't involved at all here
 * (no real GOOGLE_VISION_API_KEY in this environment, see CLAUDE.md) — layouts are synthetic, built
 * directly, since {@link HandwritingService} only ever consumes an already-parsed {@link
 * DocumentTextLayout}, never touches Vision itself (task 51's deliberate architecture split).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class HandwritingServiceIntegrationTest {

  @Autowired private HandwritingService handwritingService;
  @Autowired private EntityManager entityManager;
  @Autowired private TransactionTemplate transactionTemplate;

  private UUID studentId;
  private UUID teacherId;
  private UUID schoolId;
  private UUID classId;

  @BeforeEach
  void createStudentAndTeacher() {
    studentId = UUID.randomUUID();
    teacherId = UUID.randomUUID();
    schoolId = UUID.randomUUID();
    classId = UUID.randomUUID();
    transactionTemplate.executeWithoutResult(
        status -> {
          entityManager
              .createNativeQuery(
                  "INSERT INTO schools (id, name, address, region, district) VALUES (:id,"
                      + " 'HW Test School', 'addr', 'region', 'district')")
              .setParameter("id", schoolId)
              .executeUpdate();
          insertUser(studentId, "Test", "Student", "STUDENT");
          insertUser(teacherId, "Test", "Teacher", "TEACHER");
          // resetProfile() requires the caller to be this student's class/homeroom teacher
          // (school_classes.class_teacher_id), not just any teacher at the school.
          entityManager
              .createNativeQuery(
                  "INSERT INTO school_classes (id, school_id, grade, letter, full_name,"
                      + " class_teacher_id, academic_year) VALUES (:id, :schoolId, 9, 'A', '9-A',"
                      + " :teacherId, '2026-2027')")
              .setParameter("id", classId)
              .setParameter("schoolId", schoolId)
              .setParameter("teacherId", teacherId)
              .executeUpdate();
          entityManager
              .createNativeQuery(
                  "INSERT INTO student_profiles (id, user_id, class_id, school_id) VALUES"
                      + " (:id, :userId, :classId, :schoolId)")
              .setParameter("id", UUID.randomUUID())
              .setParameter("userId", studentId)
              .setParameter("classId", classId)
              .setParameter("schoolId", schoolId)
              .executeUpdate();
        });
  }

  private void insertUser(UUID id, String firstName, String lastName, String role) {
    entityManager
        .createNativeQuery(
            "INSERT INTO users (id, first_name, last_name, phone, password_hash, role) VALUES"
                + " (:id, :first, :last, :phone, 'x', :role)")
        .setParameter("id", id)
        .setParameter("first", firstName)
        .setParameter("last", lastName)
        .setParameter("phone", "+99890" + Math.abs(id.hashCode() % 10000000))
        .setParameter("role", role)
        .executeUpdate();
  }

  @AfterEach
  void cleanUp() {
    transactionTemplate.executeWithoutResult(
        status -> {
          entityManager
              .createNativeQuery("DELETE FROM handwriting_reset_logs WHERE student_id = :id")
              .setParameter("id", studentId)
              .executeUpdate();
          entityManager
              .createNativeQuery("DELETE FROM handwriting_profiles WHERE student_id = :id")
              .setParameter("id", studentId)
              .executeUpdate();
          entityManager
              .createNativeQuery("DELETE FROM student_profiles WHERE user_id = :id")
              .setParameter("id", studentId)
              .executeUpdate();
          entityManager
              .createNativeQuery("DELETE FROM school_classes WHERE id = :id")
              .setParameter("id", classId)
              .executeUpdate();
          entityManager
              .createNativeQuery("DELETE FROM users WHERE id IN (:s, :t)")
              .setParameter("s", studentId)
              .setParameter("t", teacherId)
              .executeUpdate();
          entityManager
              .createNativeQuery("DELETE FROM schools WHERE id = :id")
              .setParameter("id", schoolId)
              .executeUpdate();
        });
  }

  @Test
  void firstSampleCreatesReliableFalseProfileWith100PercentMatch() {
    HandwritingCheckResult result =
        handwritingService.checkAndUpdateProfile(studentId, sampleLayout(10, 10, 0.0));

    assertThat(result.matchScore()).isEqualTo(100.0f);
    assertThat(result.profileWasReliable()).isFalse();
    assertThat(result.type()).isEqualTo(PlagiarismType.CLEAN);

    Object[] row = fetchProfileRow();
    assertThat((Integer) row[0]).isEqualTo(1); // samples_count
    assertThat((Boolean) row[1]).isFalse(); // is_reliable
    assertThat(row[2]).isNotNull(); // feature_vector persisted
  }

  @Test
  void becomesReliableAfterFiveMatchingSamples() {
    for (int i = 0; i < 5; i++) {
      handwritingService.checkAndUpdateProfile(studentId, sampleLayout(10, 10, 0.0));
    }

    Object[] row = fetchProfileRow();
    assertThat((Integer) row[0]).isEqualTo(5);
    assertThat((Boolean) row[1]).isTrue();
  }

  @Test
  void matchingHandwritingScoresHighOnceReliable() {
    for (int i = 0; i < 5; i++) {
      handwritingService.checkAndUpdateProfile(studentId, sampleLayout(10, 10, 0.0));
    }

    HandwritingCheckResult result =
        handwritingService.checkAndUpdateProfile(studentId, sampleLayout(10, 10, 0.0));

    assertThat(result.profileWasReliable()).isTrue();
    assertThat(result.matchScore()).isGreaterThan(70.0f);
    assertThat(result.type()).isEqualTo(PlagiarismType.CLEAN);
  }

  @Test
  void wildlyDifferentHandwritingFlagsMismatchOnceReliable() {
    for (int i = 0; i < 5; i++) {
      handwritingService.checkAndUpdateProfile(studentId, sampleLayout(10, 10, 0.0));
    }

    // Deliberately don't try to hand-craft a "different writer" synthetic layout that's
    // guaranteed to land under the 60% threshold — HandwritingFeatureExtractorTest (task 49)
    // already proves close-vs-far inputs produce more-vs-less similar vectors; two same-shaped
    // synthetic fixtures built from a handful of scalar knobs can still end up cosine-similar
    // just from sharing several genuinely-degenerate metrics (uniform per-char height/baseline
    // in a synthetic fixture has zero variance either way, unlike a real photographed sample).
    // Instead, directly corrupt the persisted vector to something certainly dissimilar (negate
    // it — cosine similarity of a vector and its negation is exactly -1), which tests exactly
    // what this test is actually responsible for: the mismatch/threshold decision in
    // HandwritingService, not the feature extractor's discriminating power.
    negateStoredVector();

    HandwritingCheckResult result =
        handwritingService.checkAndUpdateProfile(studentId, sampleLayout(10, 10, 0.0));

    assertThat(result.profileWasReliable()).isTrue();
    assertThat(result.matchScore()).isLessThan(60.0f);
    assertThat(result.type()).isEqualTo(PlagiarismType.HANDWRITING_MISMATCH);
  }

  private void negateStoredVector() {
    String text =
        (String)
            entityManager
                .createNativeQuery(
                    "SELECT feature_vector::text FROM handwriting_profiles WHERE student_id = :id")
                .setParameter("id", studentId)
                .getSingleResult();
    String negated =
        "["
            + String.join(
                ",",
                java.util.Arrays.stream(text.substring(1, text.length() - 1).split(","))
                    .map(v -> String.valueOf(-Double.parseDouble(v)))
                    .toList())
            + "]";
    transactionTemplate.executeWithoutResult(
        status ->
            entityManager
                .createNativeQuery(
                    "UPDATE handwriting_profiles SET feature_vector = CAST(:vec AS vector) WHERE student_id = :id")
                .setParameter("vec", negated)
                .setParameter("id", studentId)
                .executeUpdate());
  }

  @Test
  void resetProfileVersionsUpAndClearsVector() {
    handwritingService.checkAndUpdateProfile(studentId, sampleLayout(10, 10, 0.0));

    var resetResult =
        handwritingService.resetProfile(
            schoolId, studentId, teacherId, ResetReason.ILLNESS, "notes");

    assertThat(resetResult.newProfileVersion()).isEqualTo("v2");
    assertThat(resetResult.resetCountThisQuarter()).isEqualTo(1);

    Object[] row = fetchProfileRow();
    assertThat((Integer) row[0]).isZero(); // samples_count reset
    assertThat(row[2]).isNull(); // feature_vector cleared

    Long logCount =
        ((Number)
                entityManager
                    .createNativeQuery(
                        "SELECT COUNT(*) FROM handwriting_reset_logs WHERE student_id = :id")
                    .setParameter("id", studentId)
                    .getSingleResult())
            .longValue();
    assertThat(logCount).isEqualTo(1);
  }

  @Test
  void resetLimitExceededAfterThreeResets() {
    for (int i = 0; i < 3; i++) {
      handwritingService.resetProfile(schoolId, studentId, teacherId, ResetReason.OTHER, null);
    }

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                handwritingService.resetProfile(
                    schoolId, studentId, teacherId, ResetReason.OTHER, null))
        .isInstanceOf(uz.academixai.interfaces.web.ApiException.class)
        .hasMessageContaining("limit");
  }

  @Test
  void resetDeniedForATeacherWhoIsNotTheClassTeacher() {
    UUID otherTeacherId = UUID.randomUUID();

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                handwritingService.resetProfile(
                    schoolId, studentId, otherTeacherId, ResetReason.OTHER, null))
        .isInstanceOf(uz.academixai.interfaces.web.ApiException.class)
        .hasMessageContaining("sinf rahbari");
  }

  @Test
  void unlockResetClearsResetCounter() {
    for (int i = 0; i < 3; i++) {
      handwritingService.resetProfile(schoolId, studentId, teacherId, ResetReason.OTHER, null);
    }

    handwritingService.unlockReset(studentId);

    Object[] row = fetchProfileRow();
    assertThat((Integer) row[3]).isZero(); // reset_count_this_quarter
  }

  private Object[] fetchProfileRow() {
    return (Object[])
        entityManager
            .createNativeQuery(
                "SELECT samples_count, is_reliable, feature_vector::text,"
                    + " reset_count_this_quarter FROM handwriting_profiles WHERE student_id = :id")
            .setParameter("id", studentId)
            .getSingleResult();
  }

  /**
   * A synthetic 2-line, 2-word-per-line layout — {@code charWidth}/{@code charHeight} control size,
   * {@code slant} shears each box horizontally. Real word/line breaks (not just one trailing
   * LINE_BREAK) so intra-word/inter-word-gap and line-spacing histograms — not just
   * width/height/aspect-ratio/slant — actually get populated and can differ between fixtures.
   */
  private static DocumentTextLayout sampleLayout(
      double charWidth, double charHeight, double slant) {
    List<CharacterBox> chars = new java.util.ArrayList<>();
    double y = 10;
    for (int line = 0; line < 2; line++) {
      double x = 10;
      for (int word = 0; word < 2; word++) {
        for (int ch = 0; ch < 4; ch++) {
          double xOffset = slant * charHeight;
          List<double[]> vertices =
              List.of(
                  new double[] {x + xOffset, y},
                  new double[] {x + charWidth + xOffset, y},
                  new double[] {x + charWidth, y + charHeight},
                  new double[] {x, y + charHeight});
          boolean lastCharOfWord = ch == 3;
          boolean lastWordOfLine = word == 1;
          CharacterBox.BreakType breakType =
              lastCharOfWord
                  ? (lastWordOfLine
                      ? CharacterBox.BreakType.LINE_BREAK
                      : CharacterBox.BreakType.SPACE)
                  : CharacterBox.BreakType.NONE;
          chars.add(new CharacterBox(String.valueOf((char) ('a' + ch)), vertices, breakType));
          x += charWidth + 2;
        }
        x += 10;
      }
      y += charHeight + 15;
    }
    return new DocumentTextLayout(chars);
  }
}
