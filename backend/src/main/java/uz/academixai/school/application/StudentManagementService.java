package uz.academixai.school.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uz.academixai.domain.Role;
import uz.academixai.domain.StudentProfile;
import uz.academixai.identity.application.PasswordPolicy;
import uz.academixai.interfaces.web.ApiException;
import uz.academixai.school.application.port.out.ClassLookup;
import uz.academixai.school.application.port.out.MemberAccountStore;
import uz.academixai.school.application.port.out.MemberAccountStore.MemberAccount;
import uz.academixai.school.application.port.out.StudentStore;
import uz.academixai.school.application.port.out.StudentStore.StudentRow;

/** academix_tz.md §2.2 "O'quvchilar" — admin CRUD over STUDENT users + student_profiles. */
@Service
public class StudentManagementService {

  private final MemberAccountStore accounts;
  private final StudentStore students;
  private final ClassLookup classes;

  public StudentManagementService(
      MemberAccountStore accounts, StudentStore students, ClassLookup classes) {
    this.accounts = accounts;
    this.students = students;
    this.classes = classes;
  }

  public List<StudentRow> list(UUID schoolId, UUID classId, String search) {
    return students.search(schoolId, classId, blankToNull(search));
  }

  // REQUIRES_NEW, not the default REQUIRED: RlsTransactionFilter wraps the WHOLE HTTP request in
  // one transaction for any authenticated request with a resolved schoolId (to cover the request
  // in a single SET LOCAL app.current_school_id). Bulk import calls this once per spreadsheet row
  // inside one request — under REQUIRED, every row's create() would join that same outer
  // transaction, so one row's caught-and-continued ApiException would still mark the whole
  // transaction rollback-only and blow up the entire commit with UnexpectedRollbackException at
  // the end of the request (confirmed by a real bulk-import commit failing this way). REQUIRES_NEW
  // makes each student an independent, immediately-committed unit of work, which is what "partial
  // success — one bad row doesn't reject the file" (academix_tz.md §2.2) actually requires.
  /** Back-compat entry (bulk import rows carry no email/password — temp password, as before). */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public StudentProfile create(
      UUID schoolId,
      String firstName,
      String lastName,
      String phone,
      UUID classId,
      String studentNumber,
      LocalDate birthDate) {
    return create(
        schoolId, firstName, lastName, phone, null, null, classId, studentNumber, birthDate);
  }

  // Admin supplies the initial password directly (and optionally an email) — the admin IS the
  // credential-delivery channel, same policy as ParentManagementService. Blank/null password
  // falls back to the old server-generated temp password (bulk import path).
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public StudentProfile create(
      UUID schoolId,
      String firstName,
      String lastName,
      String phone,
      String email,
      String password,
      UUID classId,
      String studentNumber,
      LocalDate birthDate) {
    if (password != null && !password.isBlank()) PasswordPolicy.requireValid(password);
    if (accounts.existsByPhone(phone)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_DUPLICATE_PHONE",
          "Bu telefon raqam allaqachon ro'yxatdan o'tgan.",
          "Boshqa telefon raqam kiriting yoki mavjud foydalanuvchini tekshiring.");
    }
    requireClassInSchool(schoolId, classId);

    MemberAccount student =
        accounts.save(
            new MemberAccount(
                UUID.randomUUID(),
                firstName,
                lastName,
                phone,
                email == null || email.isBlank() ? null : email,
                accounts.encodePassword(
                    password == null || password.isBlank()
                        ? accounts.generateTemporaryPassword()
                        : password),
                Role.STUDENT,
                true,
                LocalDateTime.now(),
                null,
                schoolId));

    StudentProfile saved =
        students.save(
            new StudentProfile(
                UUID.randomUUID(),
                student.id(),
                classId,
                schoolId,
                studentNumber,
                birthDate,
                0,
                0,
                0,
                null,
                true));
    classes.incrementStudentCount(classId);
    return saved;
  }

  // noRollbackFor per the CLAUDE.md rule: this participates in the request's transaction (the RLS
  // filter wraps every request) and throws a business ApiException (404 for unknown class/student)
  // before any write — without it the RLS transaction goes rollback-only and the request dies with
  // an UnexpectedRollbackException 500 instead of the intended 404 (confirmed by a real test run).
  @Transactional(noRollbackFor = ApiException.class)
  public StudentProfile transferClass(UUID schoolId, UUID studentId, UUID newClassId) {
    requireClassInSchool(schoolId, newClassId);
    StudentProfile existing =
        students
            .findInSchool(studentId, schoolId)
            .orElseThrow(StudentManagementService::studentNotFound);

    StudentProfile saved =
        students.save(
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
                existing.isActive()));
    if (existing.classId() != null && !existing.classId().equals(newClassId)) {
      classes.decrementStudentCount(existing.classId());
      classes.incrementStudentCount(newClassId);
    }
    return saved;
  }

  private void requireClassInSchool(UUID schoolId, UUID classId) {
    if (!classes.existsInSchool(classId, schoolId)) {
      throw new ApiException(
          HttpStatus.NOT_FOUND,
          "ERR_CLASS_NOT_FOUND",
          "Sinf topilmadi.",
          "ID ni tekshiring yoki ro'yxatni yangilang.");
    }
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
