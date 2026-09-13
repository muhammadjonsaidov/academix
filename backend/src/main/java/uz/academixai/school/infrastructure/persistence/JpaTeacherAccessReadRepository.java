package uz.academixai.school.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.SchoolClass;
import uz.academixai.domain.Subject;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.SubjectEntity;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.school.application.TeacherStudent;
import uz.academixai.school.application.port.out.TeacherAccessReadRepository;

/** JPA projection adapter for teacher-facing School queries. */
@Repository
public class JpaTeacherAccessReadRepository implements TeacherAccessReadRepository {

  private final SchoolClassRepository classRepository;
  private final SubjectRepository subjectRepository;
  private final StudentProfileRepository studentProfileRepository;

  public JpaTeacherAccessReadRepository(
      SchoolClassRepository classRepository,
      SubjectRepository subjectRepository,
      StudentProfileRepository studentProfileRepository) {
    this.classRepository = classRepository;
    this.subjectRepository = subjectRepository;
    this.studentProfileRepository = studentProfileRepository;
  }

  @Override
  public List<SchoolClass> findClassesById(Collection<UUID> classIds) {
    if (classIds.isEmpty()) {
      return List.of();
    }
    return classRepository.findAllById(classIds).stream().map(SchoolClassEntity::toDomain).toList();
  }

  @Override
  public List<Subject> findSubjectsById(Collection<UUID> subjectIds) {
    if (subjectIds.isEmpty()) {
      return List.of();
    }
    return subjectRepository.findAllById(subjectIds).stream().map(SubjectEntity::toDomain).toList();
  }

  @Override
  public List<TeacherStudent> findStudents(UUID schoolId, UUID classId) {
    return studentProfileRepository.searchBySchool(schoolId, classId, null).stream()
        .map(
            row ->
                new TeacherStudent(
                    row.getUserId(), row.getFirstName(), row.getLastName(), row.getStudentNumber()))
        .toList();
  }
}
