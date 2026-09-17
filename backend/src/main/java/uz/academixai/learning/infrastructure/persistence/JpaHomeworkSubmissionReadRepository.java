package uz.academixai.learning.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionEntity;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.learning.application.port.out.HomeworkSubmissionReadRepository;

/** JPA projection adapter for student-facing submission reads. */
@Repository
public class JpaHomeworkSubmissionReadRepository implements HomeworkSubmissionReadRepository {

  private final HomeworkSubmissionRepository repository;

  public JpaHomeworkSubmissionReadRepository(HomeworkSubmissionRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<HomeworkSubmission> findByAssignmentIdAndStudentId(
      UUID assignmentId, UUID studentId) {
    return repository
        .findByAssignmentIdAndStudentId(assignmentId, studentId)
        .map(HomeworkSubmissionEntity::toDomain);
  }

  @Override
  public List<HomeworkSubmission> findByStudentId(UUID studentId) {
    return repository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
        .map(HomeworkSubmissionEntity::toDomain)
        .toList();
  }

  @Override
  public Optional<HomeworkSubmission> findByIdAndSchoolId(UUID submissionId, UUID schoolId) {
    return repository
        .findByIdAndSchoolId(submissionId, schoolId)
        .map(HomeworkSubmissionEntity::toDomain);
  }

  @Override
  public List<HomeworkSubmission> findByAssignmentId(UUID assignmentId) {
    return repository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId).stream()
        .map(HomeworkSubmissionEntity::toDomain)
        .toList();
  }

  @Override
  public List<HomeworkSubmission> findByAssignmentIds(List<UUID> assignmentIds) {
    return repository.findByAssignmentIdInOrderBySubmittedAtDesc(assignmentIds).stream()
        .map(HomeworkSubmissionEntity::toDomain)
        .toList();
  }
}
