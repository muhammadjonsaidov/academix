package uz.academixai.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.academixai.domain.SubmissionStatus;

public interface HomeworkSubmissionRepository
    extends JpaRepository<HomeworkSubmissionEntity, UUID> {

  List<HomeworkSubmissionEntity> findByAssignmentIdOrderBySubmittedAtDesc(UUID assignmentId);

  List<HomeworkSubmissionEntity> findByAssignmentIdInOrderBySubmittedAtDesc(
      Collection<UUID> assignmentIds);

  List<HomeworkSubmissionEntity> findByStudentIdOrderBySubmittedAtDesc(UUID studentId);

  Optional<HomeworkSubmissionEntity> findByIdAndSchoolId(UUID id, UUID schoolId);

  Optional<HomeworkSubmissionEntity> findByAssignmentIdAndStudentId(
      UUID assignmentId, UUID studentId);

  boolean existsByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);

  long countByAssignmentId(UUID assignmentId);

  /** Atomically claims a newly accepted submission for the async AI worker. */
  @Modifying
  @Query(
      "update HomeworkSubmissionEntity submission set submission.status = :claimed "
          + "where submission.id = :submissionId and submission.status = :expected")
  int claimForAi(
      @Param("submissionId") UUID submissionId,
      @Param("expected") SubmissionStatus expected,
      @Param("claimed") SubmissionStatus claimed);

  /**
   * admin dashboard's {@code activeToday}/{@code homeworkSubmissionRate} — count of distinct
   * students who submitted at least once since {@code since}, judgment call: "active" isn't defined
   * anywhere in academix_tz.md beyond the field name, so submission activity is used as the proxy
   * (same signal {@code student_profiles.last_submission_date} already tracks).
   */
  @Query(
      value =
          "SELECT COUNT(DISTINCT student_id) FROM homework_submissions "
              + "WHERE school_id = :schoolId AND submitted_at >= :since",
      nativeQuery = true)
  int countDistinctStudentsSubmittedSince(
      @Param("schoolId") UUID schoolId, @Param("since") LocalDateTime since);

  /**
   * {@code GET /teacher/dashboard}'s {@code pendingSubmissions} — AI-processed but not yet
   * teacher-graded, scoped to this teacher's own assignments.
   */
  @Query(
      value =
          """
          SELECT COUNT(*) FROM homework_submissions hs
          JOIN homework_assignments ha ON ha.id = hs.assignment_id
          LEFT JOIN grades g ON g.submission_id = hs.id
          WHERE ha.school_id = :schoolId AND ha.teacher_id = :teacherId
            AND hs.status IN ('AI_DONE', 'AI_SKIPPED') AND g.id IS NULL
          """,
      nativeQuery = true)
  int countPendingGradeByTeacher(
      @Param("schoolId") UUID schoolId, @Param("teacherId") UUID teacherId);
}
