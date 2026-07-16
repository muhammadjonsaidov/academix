package uz.academixai.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GradeRepository extends JpaRepository<GradeEntity, UUID> {

  Optional<GradeEntity> findBySubmissionId(UUID submissionId);

  interface ClassProgressRow {
    UUID getClassId();

    String getClassName();

    int getStudentCount();

    double getAvgScore();

    long getGradedCount();
  }

  /**
   * Admin dashboard's {@code classProgressList} / {@code classes-comparison} (academix_tz.md §2.2)
   * — {@code subjectId} filter is optional (null = all subjects), scored over grades since {@code
   * since}.
   */
  @Query(
      value =
          """
          SELECT sc.id AS classId, sc.full_name AS className, sc.student_count AS studentCount,
                 COALESCE(AVG(g.score), 0) AS avgScore, COUNT(g.id) AS gradedCount
          FROM school_classes sc
          LEFT JOIN homework_assignments ha ON ha.class_id = sc.id
            AND (:subjectId IS NULL OR ha.subject_id = :subjectId)
          LEFT JOIN homework_submissions hs ON hs.assignment_id = ha.id
          LEFT JOIN grades g ON g.submission_id = hs.id AND g.graded_at >= :since
          WHERE sc.school_id = :schoolId AND sc.is_active = true
          GROUP BY sc.id, sc.full_name, sc.student_count
          ORDER BY sc.full_name
          """,
      nativeQuery = true)
  List<ClassProgressRow> classProgress(
      @Param("schoolId") UUID schoolId,
      @Param("subjectId") UUID subjectId,
      @Param("since") LocalDateTime since);

  interface TeacherRankingRow {
    UUID getTeacherId();

    String getFirstName();

    String getLastName();

    double getAvgGrade();

    long getGradedCount();
  }

  /**
   * Rating isn't defined anywhere in academix_tz.md beyond the {@code score: 4.7} example shape —
   * judgment call: average of the teacher's own {@code grades.five_point_grade} (already a 2-5
   * scale, no rescaling needed).
   */
  @Query(
      value =
          """
          SELECT u.id AS teacherId, u.first_name AS firstName, u.last_name AS lastName,
                 COALESCE(AVG(g.five_point_grade), 0) AS avgGrade, COUNT(g.id) AS gradedCount
          FROM users u
          JOIN grades g ON g.teacher_id = u.id
          JOIN homework_submissions hs ON hs.id = g.submission_id
          WHERE hs.school_id = :schoolId
          GROUP BY u.id, u.first_name, u.last_name
          ORDER BY avgGrade DESC
          """,
      nativeQuery = true)
  List<TeacherRankingRow> teacherRanking(@Param("schoolId") UUID schoolId);

  interface PeriodProgressRow {
    LocalDateTime getPeriodStart();

    double getAvgScore();

    long getGradedCount();
  }

  /**
   * {@code school-progress?period=monthly|semester} — bucketed by calendar month either way
   * (Postgres has no native "semester" truncation); {@code period} instead controls the lookback
   * window via {@code since} (judgment call, see AdminAnalyticsService).
   */
  @Query(
      value =
          """
          SELECT date_trunc('month', g.graded_at) AS periodStart,
                 COALESCE(AVG(g.score), 0) AS avgScore, COUNT(g.id) AS gradedCount
          FROM grades g
          JOIN homework_submissions hs ON hs.id = g.submission_id
          WHERE hs.school_id = :schoolId AND g.graded_at >= :since
          GROUP BY periodStart
          ORDER BY periodStart
          """,
      nativeQuery = true)
  List<PeriodProgressRow> schoolProgress(
      @Param("schoolId") UUID schoolId, @Param("since") LocalDateTime since);

  interface SubjectProgressRow {
    String getSubjectName();

    double getAvgScore();

    long getGradedCount();
  }

  /** Semester report (STUDENT type) — per-subject breakdown for one student. */
  @Query(
      value =
          """
          SELECT s.name AS subjectName, COALESCE(AVG(g.score), 0) AS avgScore,
                 COUNT(g.id) AS gradedCount
          FROM grades g
          JOIN homework_submissions hs ON hs.id = g.submission_id
          JOIN homework_assignments ha ON ha.id = hs.assignment_id
          JOIN subjects s ON s.id = ha.subject_id
          WHERE hs.school_id = :schoolId AND hs.student_id = :studentId AND g.graded_at >= :since
          GROUP BY s.name
          ORDER BY s.name
          """,
      nativeQuery = true)
  List<SubjectProgressRow> studentSubjectProgress(
      @Param("schoolId") UUID schoolId,
      @Param("studentId") UUID studentId,
      @Param("since") LocalDateTime since);

  interface StudentProgressRow {
    UUID getStudentId();

    String getFirstName();

    String getLastName();

    double getAvgScore();

    long getGradedCount();

    int getTotalXp();
  }

  /** Semester report (CLASS type) — per-student breakdown for one class. */
  @Query(
      value =
          """
          SELECT u.id AS studentId, u.first_name AS firstName, u.last_name AS lastName,
                 COALESCE(AVG(g.score), 0) AS avgScore, COUNT(g.id) AS gradedCount,
                 COALESCE(sp.total_xp, 0) AS totalXp
          FROM student_profiles sp
          JOIN users u ON u.id = sp.user_id
          LEFT JOIN homework_submissions hs ON hs.student_id = u.id AND hs.school_id = :schoolId
          LEFT JOIN grades g ON g.submission_id = hs.id AND g.graded_at >= :since
          WHERE sp.class_id = :classId AND sp.school_id = :schoolId AND sp.is_active = true
          GROUP BY u.id, u.first_name, u.last_name, sp.total_xp
          ORDER BY u.last_name, u.first_name
          """,
      nativeQuery = true)
  List<StudentProgressRow> classStudentProgress(
      @Param("schoolId") UUID schoolId,
      @Param("classId") UUID classId,
      @Param("since") LocalDateTime since);
}
