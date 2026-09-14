package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Role;
import uz.academixai.domain.User;
import uz.academixai.identity.application.PasswordPolicy;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.infrastructure.security.TempPasswordGenerator;
import uz.academixai.interfaces.web.ApiException;

/**
 * Deviation, judgment call (see SchoolContextResolver's Javadoc): academix_tz.md never documents an
 * admin invite/list flow for PSYCHOLOGIST users — without one there was no way to create a
 * psychologist account at all, blocking every endpoint in Sprint 7 (psychological signal
 * monitoring). Mirrors {@link TeacherManagementService} exactly, same {@code users.school_id}
 * deviation column, just scoped to {@code Role.PSYCHOLOGIST}.
 */
@Service
public class PsychologistManagementService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public PsychologistManagementService(
      UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public List<User> list(UUID schoolId) {
    return userRepository
        .findByRoleAndSchoolIdOrderByLastNameAscFirstNameAsc(Role.PSYCHOLOGIST, schoolId)
        .stream()
        .map(UserEntity::toDomain)
        .toList();
  }

  // Same two-mode behavior as TeacherManagementService.invite: admin-supplied password =>
  // active immediately (admin is the delivery channel); no password => temp + inactive.
  public User invite(
      UUID schoolId,
      String phone,
      String firstName,
      String lastName,
      String email,
      String password) {
    boolean hasPassword = password != null && !password.isBlank();
    if (hasPassword) PasswordPolicy.requireValid(password);
    if (userRepository.existsByPhone(phone)) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_DUPLICATE_PHONE",
          "Bu telefon raqam allaqachon ro'yxatdan o'tgan.",
          "Boshqa telefon raqam kiriting yoki mavjud foydalanuvchini tekshiring.");
    }

    UserEntity entity =
        new UserEntity(
            UUID.randomUUID(),
            firstName,
            lastName,
            phone,
            email,
            passwordEncoder.encode(hasPassword ? password : TempPasswordGenerator.generate()),
            Role.PSYCHOLOGIST,
            hasPassword,
            LocalDateTime.now(),
            null,
            schoolId);
    return userRepository.save(entity).toDomain();
  }

  public User activate(UUID schoolId, UUID psychologistId) {
    return setActive(schoolId, psychologistId, true);
  }

  public User deactivate(UUID schoolId, UUID psychologistId) {
    return setActive(schoolId, psychologistId, false);
  }

  private User setActive(UUID schoolId, UUID psychologistId, boolean active) {
    UserEntity entity = requireOwned(schoolId, psychologistId);
    UserEntity updated =
        new UserEntity(
            entity.getId(),
            entity.getFirstName(),
            entity.getLastName(),
            entity.getPhone(),
            entity.getEmail(),
            entity.getPasswordHash(),
            entity.getRole(),
            active,
            entity.getCreatedAt(),
            entity.getLastLoginAt(),
            entity.getSchoolId());
    return userRepository.save(updated).toDomain();
  }

  private UserEntity requireOwned(UUID schoolId, UUID psychologistId) {
    return userRepository
        .findByIdAndRoleAndSchoolId(psychologistId, Role.PSYCHOLOGIST, schoolId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_PSYCHOLOGIST_NOT_FOUND",
                    "Psixolog topilmadi.",
                    "ID ni tekshiring yoki ro'yxatni yangilang."));
  }
}
