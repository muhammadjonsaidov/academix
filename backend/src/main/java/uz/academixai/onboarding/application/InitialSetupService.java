package uz.academixai.onboarding.application;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.academixai.domain.Role;
import uz.academixai.domain.School;
import uz.academixai.identity.application.PasswordPolicy;
import uz.academixai.identity.application.port.out.AccountRepository;
import uz.academixai.identity.domain.Account;
import uz.academixai.interfaces.web.ApiException;
import uz.academixai.school.application.port.out.SchoolAdministrationRepository;

/**
 * One-time product bootstrap for an empty installation.
 *
 * <p>This is deliberately not a general self-registration API. Creating teachers, students, parents
 * and psychologists remains an authenticated administrator operation. The transaction creates
 * exactly one owner account and one school, then permanently closes itself as soon as any account
 * exists.
 */
@Service
public class InitialSetupService {

  private static final int DEFAULT_MONTHLY_AI_LIMIT = 1_000;

  private final AccountRepository accounts;
  private final SchoolAdministrationRepository schools;
  private final PasswordEncoder passwordEncoder;

  public InitialSetupService(
      AccountRepository accounts,
      SchoolAdministrationRepository schools,
      PasswordEncoder passwordEncoder) {
    this.accounts = accounts;
    this.schools = schools;
    this.passwordEncoder = passwordEncoder;
  }

  public boolean isAvailable() {
    return accounts.count() == 0;
  }

  @Transactional
  public Account createFirstAdministrator(
      String firstName,
      String lastName,
      String phone,
      String email,
      String password,
      String schoolName,
      String region,
      String district,
      String address) {
    if (!isAvailable()) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_SETUP_COMPLETED",
          "Boshlang'ich sozlash allaqachon yakunlangan.",
          "Tizimga mavjud administrator hisobi orqali kiring.");
    }
    validate(firstName, "Ism");
    validate(lastName, "Familiya");
    validate(phone, "Telefon raqam");
    validate(password, "Parol");
    validate(schoolName, "Maktab nomi");
    validate(region, "Viloyat");
    validate(district, "Tuman yoki shahar");
    validate(address, "Manzil");
    PasswordPolicy.requireValid(password);

    UUID adminId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    Account administrator =
        new Account(
            adminId,
            firstName.trim(),
            lastName.trim(),
            phone.trim(),
            blankToNull(email),
            passwordEncoder.encode(password),
            Role.ADMIN,
            true,
            now,
            null,
            null);
    accounts.save(administrator);

    schools.save(
        new School(
            UUID.randomUUID(),
            schoolName.trim(),
            address.trim(),
            region.trim(),
            district.trim(),
            phone.trim(),
            blankToNull(email),
            0,
            true,
            now,
            null,
            adminId,
            DEFAULT_MONTHLY_AI_LIMIT,
            0));
    return administrator;
  }

  private static void validate(String value, String field) {
    if (value == null || value.isBlank()) {
      throw validation(field + " majburiy.");
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static ApiException validation(String message) {
    return new ApiException(
        HttpStatus.BAD_REQUEST,
        "ERR_VALIDATION",
        message,
        "Maydonlarni tekshirib qayta urinib ko'ring.");
  }
}
