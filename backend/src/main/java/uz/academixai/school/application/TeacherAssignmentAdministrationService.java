package uz.academixai.school.application;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.ClassSubjectTeacher;
import uz.academixai.school.application.port.out.ClassAdministrationRepository;
import uz.academixai.school.application.port.out.SubjectCatalogRepository;
import uz.academixai.school.application.port.out.TeacherAssignmentRepository;
import uz.academixai.school.application.port.out.TeacherDirectory;
import uz.academixai.shared.error.ApiException;

/** Administrative use cases for assigning one teacher to a class and subject. */
@Service
public class TeacherAssignmentAdministrationService {

  private final TeacherAssignmentRepository assignments;
  private final ClassAdministrationRepository classes;
  private final SubjectCatalogRepository subjects;
  private final TeacherDirectory teachers;

  public TeacherAssignmentAdministrationService(
      TeacherAssignmentRepository assignments,
      ClassAdministrationRepository classes,
      SubjectCatalogRepository subjects,
      TeacherDirectory teachers) {
    this.assignments = assignments;
    this.classes = classes;
    this.subjects = subjects;
    this.teachers = teachers;
  }

  public List<ClassSubjectTeacher> list(UUID schoolId) {
    return assignments.findBySchoolId(schoolId);
  }

  public ClassSubjectTeacher create(UUID schoolId, UUID teacherId, UUID classId, UUID subjectId) {
    classes
        .findByIdAndSchoolId(classId, schoolId)
        .orElseThrow(() -> notFound("ERR_CLASS_NOT_FOUND", "Sinf topilmadi."));
    subjects
        .findByIdAndSchoolId(subjectId, schoolId)
        .orElseThrow(() -> notFound("ERR_SUBJECT_NOT_FOUND", "Fan topilmadi."));
    if (!teachers.existsTeacher(schoolId, teacherId)) {
      throw notFound("ERR_TEACHER_NOT_FOUND", "O'qituvchi topilmadi.");
    }
    if (assignments.exists(schoolId, teacherId, classId, subjectId)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_ASSIGNMENT_ALREADY_EXISTS",
          "Bu biriktirish allaqachon mavjud.",
          "Mavjud ro'yxatni tekshiring.");
    }

    return assignments.save(
        new ClassSubjectTeacher(
            UUID.randomUUID(), schoolId, classId, subjectId, teacherId, currentAcademicYear()));
  }

  public void delete(UUID schoolId, UUID assignmentId) {
    assignments
        .findByIdAndSchoolId(assignmentId, schoolId)
        .orElseThrow(() -> notFound("ERR_ASSIGNMENT_NOT_FOUND", "Biriktirish topilmadi."));
    assignments.deleteById(assignmentId);
  }

  private static ApiException notFound(String code, String message) {
    return new ApiException(
        HttpStatus.NOT_FOUND, code, message, "ID ni tekshiring yoki ro'yxatni yangilang.");
  }

  private static String currentAcademicYear() {
    LocalDate today = LocalDate.now();
    int startYear =
        today.getMonthValue() >= Month.SEPTEMBER.getValue() ? today.getYear() : today.getYear() - 1;
    return startYear + "-" + (startYear + 1);
  }
}
