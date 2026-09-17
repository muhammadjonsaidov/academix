package uz.academixai.application;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.academixai.domain.PasswordResetLog;
import uz.academixai.infrastructure.persistence.PasswordResetLogEntity;
import uz.academixai.infrastructure.persistence.PasswordResetLogRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.shared.error.ApiException;

/**
 * academix_tz.md §2.1 — class-teacher-assisted STUDENT password reset. Students have no {@code
 * email} (see {@link AuthService}'s forgot-password, which only covers ADMIN/TEACHER/PARENT/
 * PSYCHOLOGIST), so there's no email channel to reset through — this mirrors {@link
 * HandwritingService#resetProfile}'s exact ownership-check + audit-log shape (the closest real
 * analog: same teacher-initiated-on-a-student pattern), generating a temporary password the teacher
 * relays to the student directly instead of a token/link.
 *
 * <p>No forced-change-on-next-login and no reset-count limit, unlike handwriting reset — a
 * deliberate, documented judgment call (see the spec addition's own comment): this is lower-stakes
 * than handwriting biometrics (no anti-fraud property being reset), and misuse is already bounded
 * by the same ownership check (only that student's own class teacher can call this) trusted
 * elsewhere in this codebase.
 */
@Service
public class StudentPasswordResetService {

  private static final String TEMP_PASSWORD_CHARS =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
  private static final int TEMP_PASSWORD_LENGTH = 10;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final StudentProfileRepository studentProfileRepository;
  private final SchoolClassRepository classRepository;
  private final UserRepository userRepository;
  private final PasswordResetLogRepository resetLogRepository;
  private final PasswordEncoder passwordEncoder;

  public StudentPasswordResetService(
      StudentProfileRepository studentProfileRepository,
      SchoolClassRepository classRepository,
      UserRepository userRepository,
      PasswordResetLogRepository resetLogRepository,
      PasswordEncoder passwordEncoder) {
    this.studentProfileRepository = studentProfileRepository;
    this.classRepository = classRepository;
    this.userRepository = userRepository;
    this.resetLogRepository = resetLogRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * {@code noRollbackFor}: same reasoning as {@code HandwritingService.resetProfile} — the
   * ownership-check guards below throw before any write, so allowing the shared per-request
   * transaction ({@code RlsTransactionFilter}) to commit normally on a clean 403/404 is safe and
   * avoids corrupting the response for real browser clients (see CLAUDE.md's documented incident).
   */
  @Transactional(noRollbackFor = ApiException.class)
  public String resetPassword(UUID schoolId, UUID studentId, UUID teacherId) {
    requireClassTeacher(schoolId, studentId, teacherId);

    String tempPassword = generateTempPassword();
    UserEntity student =
        userRepository
            .findById(studentId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_NOT_FOUND",
                        "O'quvchi topilmadi.",
                        "ID ni tekshiring."));
    UserEntity updated =
        new UserEntity(
            student.getId(),
            student.getFirstName(),
            student.getLastName(),
            student.getPhone(),
            student.getEmail(),
            passwordEncoder.encode(tempPassword),
            student.getRole(),
            student.isActive(),
            student.getCreatedAt(),
            student.getLastLoginAt(),
            student.getSchoolId());
    userRepository.save(updated);

    PasswordResetLog log =
        new PasswordResetLog(
            UUID.randomUUID(), schoolId, studentId, teacherId, LocalDateTime.now());
    resetLogRepository.save(PasswordResetLogEntity.fromDomain(log));

    return tempPassword;
  }

  // Identical ownership check to HandwritingService.requireClassTeacher — not extracted to a
  // shared helper since the two services have no other coupling and duplicating one small
  // private method is cheaper than introducing a shared dependency between them.
  private void requireClassTeacher(UUID schoolId, UUID studentId, UUID teacherId) {
    StudentProfileEntity profile =
        studentProfileRepository
            .findByUserIdAndSchoolId(studentId, schoolId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_NOT_FOUND",
                        "O'quvchi topilmadi.",
                        "ID ni tekshiring."));
    SchoolClassEntity schoolClass =
        classRepository
            .findById(profile.getClassId())
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_NOT_FOUND",
                        "Sinf topilmadi.",
                        "ID ni tekshiring."));
    if (!teacherId.equals(schoolClass.getClassTeacherId())) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_ACCESS_DENIED",
          "Faqat sinf rahbari o'quvchi parolini reset qila oladi.",
          "Bu o'quvchining sinf rahbari bilan bog'laning.");
    }
  }

  private String generateTempPassword() {
    StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
    for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
      sb.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
    }
    return sb.toString();
  }
}
