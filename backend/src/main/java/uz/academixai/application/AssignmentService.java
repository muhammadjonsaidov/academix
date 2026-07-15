package uz.academixai.application;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.ClassSubjectTeacher;
import uz.academixai.domain.Role;
import uz.academixai.infrastructure.persistence.ClassSubjectTeacherEntity;
import uz.academixai.infrastructure.persistence.ClassSubjectTeacherRepository;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.interfaces.web.ApiException;

/** academix_tz.md §2.2 "O'qituvchi-Sinf-Fan biriktirish" — links a teacher to a class+subject. */
@Service
public class AssignmentService {

  private final ClassSubjectTeacherRepository assignmentRepository;
  private final SchoolClassRepository classRepository;
  private final SubjectRepository subjectRepository;
  private final UserRepository userRepository;

  public AssignmentService(
      ClassSubjectTeacherRepository assignmentRepository,
      SchoolClassRepository classRepository,
      SubjectRepository subjectRepository,
      UserRepository userRepository) {
    this.assignmentRepository = assignmentRepository;
    this.classRepository = classRepository;
    this.subjectRepository = subjectRepository;
    this.userRepository = userRepository;
  }

  public List<ClassSubjectTeacher> list(UUID schoolId) {
    return assignmentRepository.findBySchoolId(schoolId).stream()
        .map(ClassSubjectTeacherEntity::toDomain)
        .toList();
  }

  public ClassSubjectTeacher create(UUID schoolId, UUID teacherId, UUID classId, UUID subjectId) {
    classRepository
        .findByIdAndSchoolId(classId, schoolId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_CLASS_NOT_FOUND",
                    "Sinf topilmadi.",
                    "ID ni tekshiring yoki ro'yxatni yangilang."));
    subjectRepository
        .findByIdAndSchoolId(subjectId, schoolId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_SUBJECT_NOT_FOUND",
                    "Fan topilmadi.",
                    "ID ni tekshiring yoki ro'yxatni yangilang."));
    userRepository
        .findByIdAndRoleAndSchoolId(teacherId, Role.TEACHER, schoolId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_TEACHER_NOT_FOUND",
                    "O'qituvchi topilmadi.",
                    "ID ni tekshiring yoki ro'yxatni yangilang."));

    if (assignmentRepository.existsBySchoolIdAndClassIdAndSubjectIdAndTeacherId(
        schoolId, classId, subjectId, teacherId)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_ASSIGNMENT_ALREADY_EXISTS",
          "Bu biriktirish allaqachon mavjud.",
          "Mavjud ro'yxatni tekshiring.");
    }

    ClassSubjectTeacher assignment =
        new ClassSubjectTeacher(
            UUID.randomUUID(), schoolId, classId, subjectId, teacherId, currentAcademicYear());
    return assignmentRepository.save(ClassSubjectTeacherEntity.fromDomain(assignment)).toDomain();
  }

  public void delete(UUID schoolId, UUID assignmentId) {
    ClassSubjectTeacherEntity entity =
        assignmentRepository
            .findByIdAndSchoolId(assignmentId, schoolId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_ASSIGNMENT_NOT_FOUND",
                        "Biriktirish topilmadi.",
                        "ID ni tekshiring yoki ro'yxatni yangilang."));
    assignmentRepository.delete(entity);
  }

  // Same known-gap reasoning as SchoolClassService.currentAcademicYear() — no formula specified
  // anywhere in the spec docs.
  private static String currentAcademicYear() {
    LocalDate today = LocalDate.now();
    int startYear =
        today.getMonthValue() >= Month.SEPTEMBER.getValue() ? today.getYear() : today.getYear() - 1;
    return startYear + "-" + (startYear + 1);
  }
}
