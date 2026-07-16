package uz.academixai.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
