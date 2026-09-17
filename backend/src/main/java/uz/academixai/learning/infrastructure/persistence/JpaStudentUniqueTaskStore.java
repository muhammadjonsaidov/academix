package uz.academixai.learning.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.StudentUniqueTask;
import uz.academixai.infrastructure.persistence.StudentUniqueTaskEntity;
import uz.academixai.infrastructure.persistence.StudentUniqueTaskRepository;
import uz.academixai.learning.application.port.out.StudentUniqueTaskStore;

/** Transitional JPA adapter over the existing student_unique_tasks schema. */
@Repository
public class JpaStudentUniqueTaskStore implements StudentUniqueTaskStore {

  private final StudentUniqueTaskRepository repository;

  public JpaStudentUniqueTaskStore(StudentUniqueTaskRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<StudentUniqueTask> findByAssignmentId(UUID assignmentId) {
    return repository.findByAssignmentId(assignmentId).stream()
        .map(StudentUniqueTaskEntity::toDomain)
        .toList();
  }

  @Override
  public List<StudentUniqueTask> findUnflaggedByAssignmentId(UUID assignmentId) {
    return repository.findByAssignmentIdAndFlaggedForReviewFalse(assignmentId).stream()
        .map(StudentUniqueTaskEntity::toDomain)
        .toList();
  }

  @Override
  public Optional<StudentUniqueTask> findByAssignmentIdAndStudentId(
      UUID assignmentId, UUID studentId) {
    return repository
        .findByAssignmentIdAndStudentId(assignmentId, studentId)
        .map(StudentUniqueTaskEntity::toDomain);
  }

  @Override
  public Optional<StudentUniqueTask> findByIdAndAssignmentId(UUID taskId, UUID assignmentId) {
    return repository
        .findByIdAndAssignmentId(taskId, assignmentId)
        .map(StudentUniqueTaskEntity::toDomain);
  }

  @Override
  public StudentUniqueTask save(StudentUniqueTask task) {
    return repository.save(StudentUniqueTaskEntity.fromDomain(task)).toDomain();
  }
}
