package uz.academixai.identity.application;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.academixai.identity.application.port.out.AccountRepository;
import uz.academixai.identity.application.port.out.PasswordResetLogStore;
import uz.academixai.identity.application.port.out.StudentClassOwnership;
import uz.academixai.identity.domain.Account;
import uz.academixai.shared.error.ApiException;

/**
 * academix_tz.md §2.1 — class-teacher-assisted STUDENT password reset. Students have no {@code
 * email} (the email-based flow only covers ADMIN/TEACHER/PARENT/PSYCHOLOGIST), so there's no email
 * channel to reset through — this mirrors the handwriting reset's exact ownership-check + audit-log
 * shape (the closest real analog: same teacher-initiated-on-a-student pattern), generating a
 * temporary password the teacher relays to the student directly instead of a token/link.
 *
 * <p>No forced-change-on-next-login and no reset-count limit, unlike handwriting reset — a
 * deliberate, documented judgment call (see the spec addition's own comment): this is lower-stakes
 * than handwriting biometrics (no anti-fraud property being reset), and misuse is already bounded
 * by the same ownership check (only that student's own class teacher can call this) trusted
 * elsewhere in this codebase.
 *
 * <p>Moved here from the legacy {@code application} package: setting a password is Identity's use
 * case, and the three facts it needs from elsewhere — the student's class, that class' teacher, and
 * the audit log — are ports now. Identity already owned the account port, so this deleted four
 * dependencies and added two.
 */
@Service
public class StudentPasswordResetService {

  private static final String TEMP_PASSWORD_CHARS =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
  private static final int TEMP_PASSWORD_LENGTH = 10;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final StudentClassOwnership ownership;
  private final AccountRepository accounts;
  private final PasswordResetLogStore resetLogs;
  private final PasswordEncoder passwordEncoder;

  public StudentPasswordResetService(
      StudentClassOwnership ownership,
      AccountRepository accounts,
      PasswordResetLogStore resetLogs,
      PasswordEncoder passwordEncoder) {
    this.ownership = ownership;
    this.accounts = accounts;
    this.resetLogs = resetLogs;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * {@code noRollbackFor}: same reasoning as the handwriting reset — the ownership-check guards
   * below throw before any write, so allowing the shared per-request transaction ({@code
   * RlsTransactionFilter}) to commit normally on a clean 403/404 is safe and avoids corrupting the
   * response for real browser clients (see CLAUDE.md's documented incident).
   */
  @Transactional(noRollbackFor = ApiException.class)
  public String resetPassword(UUID schoolId, UUID studentId, UUID teacherId) {
    requireClassTeacher(schoolId, studentId, teacherId);

    String tempPassword = generateTempPassword();
    Account student = accounts.findById(studentId).orElseThrow(() -> studentNotFound());
    accounts.save(student.withPasswordHash(passwordEncoder.encode(tempPassword)));
    resetLogs.record(schoolId, studentId, teacherId, LocalDateTime.now());

    return tempPassword;
  }

  private void requireClassTeacher(UUID schoolId, UUID studentId, UUID teacherId) {
    UUID classId = ownership.classIdOf(schoolId, studentId).orElseThrow(() -> studentNotFound());
    UUID classTeacherId =
        ownership
            .classTeacherOf(classId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_NOT_FOUND",
                        "Sinf topilmadi.",
                        "ID ni tekshiring."));
    if (!teacherId.equals(classTeacherId)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Faqat sinf rahbari o'quvchi parolini reset qila oladi.",
          "Bu o'quvchining sinf rahbari bilan bog'laning.");
    }
  }

  private static ApiException studentNotFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND, "ERR_NOT_FOUND", "O'quvchi topilmadi.", "ID ni tekshiring.");
  }

  private String generateTempPassword() {
    StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
    for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
      sb.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
    }
    return sb.toString();
  }
}
