package uz.academixai.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.SchoolClass;
import uz.academixai.domain.Subject;
import uz.academixai.infrastructure.persistence.ClassSubjectTeacherEntity;
import uz.academixai.infrastructure.persistence.ClassSubjectTeacherRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.StudentProfileRepository.StudentListRow;
import uz.academixai.infrastructure.persistence.SubjectEntity;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §2.3 "Mening sinflarim va fanlarim" — derives what a teacher can see/act on purely
 * from {@code class_subject_teachers}, since {@code users} carries no per-teacher class/subject
 * link of its own.
 */
@Service
public class TeacherContextService {

  private final ClassSubjectTeacherRepository assignmentRepository;
  private final SchoolClassRepository classRepository;
  private final SubjectRepository subjectRepository;
  private final StudentProfileRepository studentProfileRepository;

  public TeacherContextService(
      ClassSubjectTeacherRepository assignmentRepository,
      SchoolClassRepository classRepository,
      SubjectRepository subjectRepository,
      StudentProfileRepository studentProfileRepository) {
    this.assignmentRepository = assignmentRepository;
    this.classRepository = classRepository;
    this.subjectRepository = subjectRepository;
    this.studentProfileRepository = studentProfileRepository;
  }

  public List<SchoolClass> myClasses(UUID schoolId, UUID teacherId) {
    Set<UUID> classIds =
        assignmentRepository.findBySchoolIdAndTeacherId(schoolId, teacherId).stream()
            .map(ClassSubjectTeacherEntity::getClassId)
            .collect(Collectors.toSet());
    return classRepository.findAllById(classIds).stream().map(SchoolClassEntity::toDomain).toList();
  }

  public List<Subject> mySubjects(UUID schoolId, UUID teacherId) {
    Set<UUID> subjectIds =
        assignmentRepository.findBySchoolIdAndTeacherId(schoolId, teacherId).stream()
            .map(ClassSubjectTeacherEntity::getSubjectId)
            .collect(Collectors.toSet());
    return subjectRepository.findAllById(subjectIds).stream().map(SubjectEntity::toDomain).toList();
  }

  public List<StudentListRow> classStudents(UUID schoolId, UUID teacherId, UUID classId) {
    requireAssignedToClass(schoolId, teacherId, classId);
    return studentProfileRepository.searchBySchool(schoolId, classId, null);
  }

  /** Used by {@link HomeworkService} to gate creating/reading homework for a class+subject. */
  public void requireAssignedToClassAndSubject(
      UUID schoolId, UUID teacherId, UUID classId, UUID subjectId) {
    boolean assigned =
        assignmentRepository.existsBySchoolIdAndTeacherIdAndClassIdAndSubjectId(
            schoolId, teacherId, classId, subjectId);
    if (!assigned) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_NOT_ASSIGNED",
          "Siz bu sinf/fanga biriktirilmagansiz.",
          "Admin bilan bog'laning.");
    }
  }

  private void requireAssignedToClass(UUID schoolId, UUID teacherId, UUID classId) {
    boolean assigned =
        assignmentRepository.findBySchoolIdAndTeacherId(schoolId, teacherId).stream()
            .anyMatch(a -> a.getClassId().equals(classId));
    if (!assigned) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_NOT_ASSIGNED",
          "Siz bu sinfga biriktirilmagansiz.",
          "Admin bilan bog'laning.");
    }
  }
}
