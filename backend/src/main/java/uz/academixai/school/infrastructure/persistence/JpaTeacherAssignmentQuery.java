package uz.academixai.school.infrastructure.persistence;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import uz.academixai.infrastructure.persistence.ClassSubjectTeacherEntity;
import uz.academixai.infrastructure.persistence.ClassSubjectTeacherRepository;
import uz.academixai.school.application.port.out.TeacherAssignmentQuery;

/** JPA adapter for School's teacher assignment read boundary. */
@Repository
public class JpaTeacherAssignmentQuery implements TeacherAssignmentQuery {

  private final ClassSubjectTeacherRepository assignmentRepository;

  public JpaTeacherAssignmentQuery(ClassSubjectTeacherRepository assignmentRepository) {
    this.assignmentRepository = assignmentRepository;
  }

  @Override
  public Set<UUID> classIds(UUID schoolId, UUID teacherId) {
    return assignmentRepository.findBySchoolIdAndTeacherId(schoolId, teacherId).stream()
        .map(ClassSubjectTeacherEntity::getClassId)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<UUID> subjectIds(UUID schoolId, UUID teacherId) {
    return assignmentRepository.findBySchoolIdAndTeacherId(schoolId, teacherId).stream()
        .map(ClassSubjectTeacherEntity::getSubjectId)
        .collect(Collectors.toSet());
  }

  @Override
  public boolean isAssignedToClass(UUID schoolId, UUID teacherId, UUID classId) {
    return assignmentRepository.findBySchoolIdAndTeacherId(schoolId, teacherId).stream()
        .anyMatch(assignment -> assignment.getClassId().equals(classId));
  }

  @Override
  public boolean isAssignedToClassAndSubject(
      UUID schoolId, UUID teacherId, UUID classId, UUID subjectId) {
    return assignmentRepository.existsBySchoolIdAndTeacherIdAndClassIdAndSubjectId(
        schoolId, teacherId, classId, subjectId);
  }
}
