package uz.academixai.school.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.ClassSubjectTeacher;
import uz.academixai.infrastructure.persistence.ClassSubjectTeacherEntity;
import uz.academixai.infrastructure.persistence.ClassSubjectTeacherRepository;
import uz.academixai.school.application.port.out.TeacherAssignmentRepository;

/** JPA write adapter for teacher-class-subject membership. */
@Repository
public class JpaTeacherAssignmentRepository implements TeacherAssignmentRepository {

  private final ClassSubjectTeacherRepository repository;

  public JpaTeacherAssignmentRepository(ClassSubjectTeacherRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<ClassSubjectTeacher> findBySchoolId(UUID schoolId) {
    return repository.findBySchoolId(schoolId).stream()
        .map(ClassSubjectTeacherEntity::toDomain)
        .toList();
  }

  @Override
  public Optional<ClassSubjectTeacher> findByIdAndSchoolId(UUID assignmentId, UUID schoolId) {
    return repository
        .findByIdAndSchoolId(assignmentId, schoolId)
        .map(ClassSubjectTeacherEntity::toDomain);
  }

  @Override
  public boolean exists(UUID schoolId, UUID teacherId, UUID classId, UUID subjectId) {
    return repository.existsBySchoolIdAndTeacherIdAndClassIdAndSubjectId(
        schoolId, teacherId, classId, subjectId);
  }

  @Override
  public ClassSubjectTeacher save(ClassSubjectTeacher assignment) {
    return repository.save(ClassSubjectTeacherEntity.fromDomain(assignment)).toDomain();
  }

  @Override
  public void deleteById(UUID assignmentId) {
    repository.deleteById(assignmentId);
  }
}
