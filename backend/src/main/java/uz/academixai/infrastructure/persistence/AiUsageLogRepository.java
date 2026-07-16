package uz.academixai.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLogEntity, UUID> {

  interface ByClassRow {
    UUID getClassId();

    String getClassName();

    long getCallCount();
  }

  @Query(
      value =
          """
          SELECT sc.id AS classId, sc.full_name AS className, COUNT(l.id) AS callCount
          FROM ai_usage_log l
          JOIN school_classes sc ON sc.id = l.class_id
          WHERE l.school_id = :schoolId AND l.created_at >= :since
          GROUP BY sc.id, sc.full_name
          ORDER BY callCount DESC
          """,
      nativeQuery = true)
  List<ByClassRow> usageByClass(
      @Param("schoolId") UUID schoolId, @Param("since") LocalDateTime since);

  interface BySubjectRow {
    UUID getSubjectId();

    String getSubjectName();

    long getCallCount();
  }

  @Query(
      value =
          """
          SELECT s.id AS subjectId, s.name AS subjectName, COUNT(l.id) AS callCount
          FROM ai_usage_log l
          JOIN subjects s ON s.id = l.subject_id
          WHERE l.school_id = :schoolId AND l.created_at >= :since
          GROUP BY s.id, s.name
          ORDER BY callCount DESC
          """,
      nativeQuery = true)
  List<BySubjectRow> usageBySubject(
      @Param("schoolId") UUID schoolId, @Param("since") LocalDateTime since);

  interface ByTeacherRow {
    UUID getTeacherId();

    String getFirstName();

    String getLastName();

    long getCallCount();
  }

  @Query(
      value =
          """
          SELECT u.id AS teacherId, u.first_name AS firstName, u.last_name AS lastName,
                 COUNT(l.id) AS callCount
          FROM ai_usage_log l
          JOIN users u ON u.id = l.teacher_id
          WHERE l.school_id = :schoolId AND l.created_at >= :since
          GROUP BY u.id, u.first_name, u.last_name
          ORDER BY callCount DESC
          """,
      nativeQuery = true)
  List<ByTeacherRow> usageByTeacher(
      @Param("schoolId") UUID schoolId, @Param("since") LocalDateTime since);
}
