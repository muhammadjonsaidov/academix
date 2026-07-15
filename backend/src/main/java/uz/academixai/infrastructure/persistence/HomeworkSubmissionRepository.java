package uz.academixai.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
