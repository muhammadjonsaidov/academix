package uz.academixai.school.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Role;
import uz.academixai.domain.User;
import uz.academixai.identity.application.PasswordPolicy;
import uz.academixai.school.application.port.out.MemberAccountStore;
import uz.academixai.school.application.port.out.MemberAccountStore.MemberAccount;
import uz.academixai.shared.error.ApiException;

/**
 * Invite/activate/deactivate for the school's staff accounts.
 *
 * <p>Teachers and psychologists used to have a class each (and their controllers a service each)
 * holding byte-identical logic that differed only in the {@link Role} constant and the error
 * strings. One role-parameterised service replaces both, so a third staff role is a constant rather
 * than a second copy — the duplicated pair was the clearest sign this belonged in School.
 *
 * <p>Two invite modes (DEVIATION on top of §2.2's password-less invite): when the admin supplies a
 * password, the account is created ACTIVE immediately — the admin is the credential-delivery
 * channel and hands the password over; the staff member changes it later via {@code
 * /auth/change-password}. Without a password the old behavior stands: server-generated temp
 * password + inactive until {@code /activate} (no delivery channel — known gap).
 */
@Service
public class StaffManagementService {

  private final MemberAccountStore accounts;

  public StaffManagementService(MemberAccountStore accounts) {
    this.accounts = accounts;
  }

  public List<User> list(UUID schoolId, Role role) {
    requireManageableRole(role);
    return accounts.findByRoleInSchool(role, schoolId).stream()
        .map(StaffManagementService::toUser)
        .toList();
  }

  public User invite(
      UUID schoolId,
      Role role,
      String phone,
      String firstName,
      String lastName,
      String email,
      String password) {
    requireManageableRole(role);
    boolean hasPassword = password != null && !password.isBlank();
    if (hasPassword) PasswordPolicy.requireValid(password);
    if (accounts.existsByPhone(phone)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_DUPLICATE_PHONE",
          "Bu telefon raqam allaqachon ro'yxatdan o'tgan.",
          "Boshqa telefon raqam kiriting yoki mavjud foydalanuvchini tekshiring.");
    }

    return toUser(
        accounts.save(
            new MemberAccount(
                UUID.randomUUID(),
                firstName,
                lastName,
                phone,
                email,
                accounts.encodePassword(
                    hasPassword ? password : accounts.generateTemporaryPassword()),
                role,
                hasPassword,
                LocalDateTime.now(),
                null,
                schoolId)));
  }

  public User activate(UUID schoolId, Role role, UUID staffId) {
    return setActive(schoolId, role, staffId, true);
  }

  public User deactivate(UUID schoolId, Role role, UUID staffId) {
    return setActive(schoolId, role, staffId, false);
  }

  private User setActive(UUID schoolId, Role role, UUID staffId, boolean active) {
    MemberAccount account = requireOwned(schoolId, role, staffId);
    return toUser(
        accounts.save(
            new MemberAccount(
                account.id(),
                account.firstName(),
                account.lastName(),
                account.phone(),
                account.email(),
                account.passwordHash(),
                account.role(),
                active,
                account.createdAt(),
                account.lastLoginAt(),
                account.schoolId())));
  }

  private MemberAccount requireOwned(UUID schoolId, Role role, UUID staffId) {
    return accounts.findInSchool(staffId, role, schoolId).orElseThrow(() -> staffNotFound(role));
  }

  /** Staff only: students, parents and admins are managed by their own use cases. */
  private static void requireManageableRole(Role role) {
    if (role != Role.TEACHER && role != Role.PSYCHOLOGIST) {
      throw new IllegalArgumentException(
          "StaffManagementService manages TEACHER and PSYCHOLOGIST accounts, not " + role);
    }
  }

  private static ApiException staffNotFound(Role role) {
    if (role == Role.PSYCHOLOGIST) {
      return new ApiException(
          HttpStatus.NOT_FOUND,
          "ERR_PSYCHOLOGIST_NOT_FOUND",
          "Psixolog topilmadi.",
          "ID ni tekshiring yoki ro'yxatni yangilang.");
    }
    return new ApiException(
        HttpStatus.NOT_FOUND,
        "ERR_TEACHER_NOT_FOUND",
        "O'qituvchi topilmadi.",
        "ID ni tekshiring yoki ro'yxatni yangilang.");
  }

  private static User toUser(MemberAccount account) {
    return new User(
        account.id(),
        account.firstName(),
        account.lastName(),
        account.phone(),
        account.email(),
        account.passwordHash(),
        account.role(),
        account.isActive(),
        account.createdAt(),
        account.lastLoginAt());
  }
}
