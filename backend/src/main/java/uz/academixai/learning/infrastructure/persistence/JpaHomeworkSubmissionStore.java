package uz.academixai.learning.infrastructure.persistence;

import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionEntity;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.learning.application.port.out.HomeworkSubmissionStore;

/** Transitional JPA adapter for accepted homework submissions. */
@Repository
public class JpaHomeworkSubmissionStore implements HomeworkSubmissionStore {

  private final HomeworkSubmissionRepository repository;

  public JpaHomeworkSubmissionStore(HomeworkSubmissionRepository repository) {
    this.repository = repository;
  }

  @Override
  public boolean existsByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId) {
    return repository.existsByAssignmentIdAndStudentId(assignmentId, studentId);
  }

  @Override
  public HomeworkSubmission save(HomeworkSubmission submission) {
    return repository.save(HomeworkSubmissionEntity.fromDomain(submission)).toDomain();
  }
}
