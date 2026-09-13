package uz.academixai.learning.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.HomeworkAssignment;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentEntity;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentRepository;
import uz.academixai.learning.application.port.out.HomeworkAssignmentStore;

/** Transitional JPA adapter over the existing homework_assignments schema. */
@Repository
public class JpaHomeworkAssignmentStore implements HomeworkAssignmentStore {

  private final HomeworkAssignmentRepository repository;

  public JpaHomeworkAssignmentStore(HomeworkAssignmentRepository repository) {
    this.repository = repository;
  }

  @Override
  public HomeworkAssignment save(HomeworkAssignment assignment) {
    return repository.save(HomeworkAssignmentEntity.fromDomain(assignment)).toDomain();
  }

  @Override
  public List<HomeworkAssignment> findBySchoolIdAndTeacherId(UUID schoolId, UUID teacherId) {
    return repository.findBySchoolIdAndTeacherIdOrderByDeadlineAtDesc(schoolId, teacherId).stream()
        .map(HomeworkAssignmentEntity::toDomain)
        .toList();
  }

  @Override
  public Optional<HomeworkAssignment> findByIdAndSchoolId(UUID assignmentId, UUID schoolId) {
    return repository
        .findByIdAndSchoolId(assignmentId, schoolId)
        .map(HomeworkAssignmentEntity::toDomain);
  }

  @Override
  public void delete(HomeworkAssignment assignment) {
    repository.delete(HomeworkAssignmentEntity.fromDomain(assignment));
  }
}
