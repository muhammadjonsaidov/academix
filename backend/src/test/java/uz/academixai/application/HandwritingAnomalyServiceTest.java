package uz.academixai.application;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;
import uz.academixai.TestcontainersConfiguration;

/**
 * Real Postgres — no mocks, matching this project's Testcontainers discipline. Builds the exact two
 * flagged-pattern scenarios from backend_tdd.md §6.5: a student reset 2+ times, and a teacher whose
 * reset count is a statistical outlier vs. their peers.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class HandwritingAnomalyServiceTest {

  @Autowired private HandwritingAnomalyService anomalyService;
  @Autowired private EntityManager entityManager;
  @Autowired private TransactionTemplate transactionTemplate;

  private final List<UUID> createdUserIds = new ArrayList<>();
  private final List<UUID> createdProfileStudentIds = new ArrayList<>();
  private UUID schoolId;

  @AfterEach
  void cleanUp() {
    transactionTemplate.executeWithoutResult(
        status -> {
          entityManager.createNativeQuery("DELETE FROM handwriting_reset_logs").executeUpdate();
          for (UUID studentId : createdProfileStudentIds) {
            entityManager
                .createNativeQuery("DELETE FROM handwriting_profiles WHERE student_id = :id")
                .setParameter("id", studentId)
                .executeUpdate();
          }
          for (UUID userId : createdUserIds) {
            entityManager
                .createNativeQuery("DELETE FROM users WHERE id = :id")
                .setParameter("id", userId)
                .executeUpdate();
          }
          if (schoolId != null) {
            entityManager
                .createNativeQuery("DELETE FROM schools WHERE id = :id")
                .setParameter("id", schoolId)
                .executeUpdate();
          }
        });
    createdUserIds.clear();
    createdProfileStudentIds.clear();
    schoolId = null;
  }

  private UUID school() {
    if (schoolId == null) {
      schoolId = UUID.randomUUID();
      UUID id = schoolId;
      transactionTemplate.executeWithoutResult(
          status ->
              entityManager
                  .createNativeQuery(
                      "INSERT INTO schools (id, name, address, region, district) VALUES (:id,"
                          + " 'Anomaly Test School', 'addr', 'region', 'district')")
                  .setParameter("id", id)
                  .executeUpdate());
    }
    return schoolId;
  }

  @Test
  void flagsStudentsWithTwoOrMoreResetsAndOutlierTeachers() {
    UUID quietStudent = insertUser("Quiet", "Student", "STUDENT");
    UUID flaggedStudent = insertUser("Flagged", "Student", "STUDENT");
    insertProfile(quietStudent, 1);
    insertProfile(flaggedStudent, 2);

    // 5 "normal" teachers with 1 reset each, one outlier with 20 — the population stddev used
    // here includes the outlier itself (inflating it), so a mild split (e.g. 10 vs. five 1s)
    // doesn't clear z > 2; this separation does (z ≈ 2.24, computed by hand against the same
    // population-stddev formula outlierTeachers() uses).
    UUID normalTeacherA = insertUser("Normal", "TeacherA", "TEACHER");
    UUID normalTeacherB = insertUser("Normal", "TeacherB", "TEACHER");
    UUID normalTeacherC = insertUser("Normal", "TeacherC", "TEACHER");
    UUID normalTeacherD = insertUser("Normal", "TeacherD", "TEACHER");
    UUID normalTeacherE = insertUser("Normal", "TeacherE", "TEACHER");
    UUID outlierTeacher = insertUser("Outlier", "Teacher", "TEACHER");
    UUID someStudent = insertUser("Some", "Student", "STUDENT");
    insertResetLog(someStudent, normalTeacherA);
    insertResetLog(someStudent, normalTeacherB);
    insertResetLog(someStudent, normalTeacherC);
    insertResetLog(someStudent, normalTeacherD);
    insertResetLog(someStudent, normalTeacherE);
    for (int i = 0; i < 20; i++) {
      insertResetLog(someStudent, outlierTeacher);
    }

    var report = anomalyService.computeAnomalies();

    assertThat(report.flaggedStudents()).contains(flaggedStudent).doesNotContain(quietStudent);
    assertThat(report.flaggedTeachers())
        .contains(outlierTeacher)
        .doesNotContain(
            normalTeacherA, normalTeacherB, normalTeacherC, normalTeacherD, normalTeacherE);
  }

  @Test
  void noOutlierFlaggedWhenAllTeachersResetEqually() {
    UUID teacherA = insertUser("Even", "TeacherA", "TEACHER");
    UUID teacherB = insertUser("Even", "TeacherB", "TEACHER");
    UUID someStudent = insertUser("Some", "Student", "STUDENT");
    insertResetLog(someStudent, teacherA);
    insertResetLog(someStudent, teacherB);

    var report = anomalyService.computeAnomalies();

    assertThat(report.flaggedTeachers()).doesNotContain(teacherA, teacherB);
  }

  private UUID insertUser(String firstName, String lastName, String role) {
    UUID id = UUID.randomUUID();
    createdUserIds.add(id);
    transactionTemplate.executeWithoutResult(
        status ->
            entityManager
                .createNativeQuery(
                    "INSERT INTO users (id, first_name, last_name, phone, password_hash, role)"
                        + " VALUES (:id, :first, :last, :phone, 'x', :role)")
                .setParameter("id", id)
                .setParameter("first", firstName)
                .setParameter("last", lastName)
                .setParameter("phone", "+9989" + Math.abs(id.hashCode() % 100000000))
                .setParameter("role", role)
                .executeUpdate());
    return id;
  }

  private void insertProfile(UUID studentId, int resetCount) {
    createdProfileStudentIds.add(studentId);
    transactionTemplate.executeWithoutResult(
        status ->
            entityManager
                .createNativeQuery(
                    "INSERT INTO handwriting_profiles (id, student_id, reset_count_this_quarter)"
                        + " VALUES (:id, :studentId, :count)")
                .setParameter("id", UUID.randomUUID())
                .setParameter("studentId", studentId)
                .setParameter("count", resetCount)
                .executeUpdate());
  }

  private void insertResetLog(UUID studentId, UUID teacherId) {
    transactionTemplate.executeWithoutResult(
        status ->
            entityManager
                .createNativeQuery(
                    "INSERT INTO handwriting_reset_logs (id, school_id, student_id, teacher_id,"
                        + " reason, reset_at) VALUES (:id, :schoolId, :studentId, :teacherId,"
                        + " 'OTHER', :resetAt)")
                .setParameter("id", UUID.randomUUID())
                .setParameter("schoolId", school())
                .setParameter("studentId", studentId)
                .setParameter("teacherId", teacherId)
                .setParameter("resetAt", LocalDateTime.now())
                .executeUpdate());
  }
}
