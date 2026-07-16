package uz.academixai.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uz.academixai.domain.Role;
import uz.academixai.domain.StudentProfile;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.StudentProfileRepository.StudentListRow;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.infrastructure.security.TempPasswordGenerator;
import uz.academixai.interfaces.web.ApiException;

/** academix_tz.md §2.2 "O'quvchilar" — admin CRUD over STUDENT users + student_profiles. */
@Service
public class StudentManagementService {

  private final UserRepository userRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final SchoolClassRepository classRepository;
  private final PasswordEncoder passwordEncoder;

  public StudentManagementService(
      UserRepository userRepository,
      StudentProfileRepository studentProfileRepository,
      SchoolClassRepository classRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.classRepository = classRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public List<StudentListRow> list(UUID schoolId, UUID classId, String search) {
    return studentProfileRepository.searchBySchool(schoolId, classId, blankToNull(search));
  }

  // REQUIRES_NEW, not the default REQUIRED: RlsTransactionFilter wraps the WHOLE HTTP request in
  // one transaction for any authenticated request with a resolved schoolId (to cover the request
  // in a single SET LOCAL app.current_school_id). Bulk import calls this once per spreadsheet row
  // inside one request — under REQUIRED, every row's create() would join that same outer
  // transaction, so one row's caught-and-continued ApiException would still mark the whole
  // transaction rollback-only and blow up the entire commit with UnexpectedRollbackException at
  // the end of the request (confirmed by a real bulk-import commit failing this way). REQUIRES_NEW
  // makes each student an independent, immediately-committed unit of work, which is what "partial
  // success — one bad row doesn't reject the file" (academix_tz.md §2.2) actually requires. Safe
  // RLS-wise: neither users nor student_profiles is RLS-enabled (see CLAUDE.md "Backend
  // architecture"), so running this on a separate connection from the outer SET LOCAL is fine.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public StudentProfile create(
      UUID schoolId,
      String firstName,
      String lastName,
      String phone,
      UUID classId,
      String studentNumber,
      LocalDate birthDate) {
    if (userRepository.existsByPhone(phone)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_DUPLICATE_PHONE",
          "Bu telefon raqam allaqachon ro'yxatdan o'tgan.",
          "Boshqa telefon raqam kiriting yoki mavjud foydalanuvchini tekshiring.");
    }
    requireClassInSchool(schoolId, classId);

    UserEntity userEntity =
        new UserEntity(
            UUID.randomUUID(),
            firstName,
            lastName,
            phone,
            null,
            passwordEncoder.encode(TempPasswordGenerator.generate()),
            Role.STUDENT,
            true,
            LocalDateTime.now(),
            null);
    UUID userId = userRepository.save(userEntity).getId();

    StudentProfile profile =
        new StudentProfile(
            UUID.randomUUID(),
            userId,
            classId,
            schoolId,
            studentNumber,
            birthDate,
            0,
            0,
            0,
            null,
            true);
    StudentProfile saved =
        studentProfileRepository.save(StudentProfileEntity.fromDomain(profile)).toDomain();
    classRepository.incrementStudentCount(classId);
    return saved;
  }

  @Transactional
  public StudentProfile transferClass(UUID schoolId, UUID studentId, UUID newClassId) {
    requireClassInSchool(schoolId, newClassId);
    StudentProfile existing =
        studentProfileRepository
            .findByUserIdAndSchoolId(studentId, schoolId)
            .orElseThrow(StudentManagementService::studentNotFound)
            .toDomain();

    StudentProfile updated =
        new StudentProfile(
            existing.id(),
            existing.userId(),
            newClassId,
            schoolId,
            existing.studentNumber(),
            existing.birthDate(),
            existing.totalXp(),
            existing.currentStreak(),
            existing.maxStreak(),
            existing.lastSubmissionDate(),
            existing.isActive());
    StudentProfile saved =
        studentProfileRepository.save(StudentProfileEntity.fromDomain(updated)).toDomain();
    if (existing.classId() != null && !existing.classId().equals(newClassId)) {
      classRepository.decrementStudentCount(existing.classId());
      classRepository.incrementStudentCount(newClassId);
    }
    return saved;
  }

  private void requireClassInSchool(UUID schoolId, UUID classId) {
    classRepository
        .findByIdAndSchoolId(classId, schoolId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_CLASS_NOT_FOUND",
                    "Sinf topilmadi.",
                    "ID ni tekshiring yoki ro'yxatni yangilang."));
  }

  private static ApiException studentNotFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        "ERR_STUDENT_NOT_FOUND",
        "O'quvchi topilmadi.",
        "ID ni tekshiring yoki ro'yxatni yangilang.");
  }

  private static String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }
}
