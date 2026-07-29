package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Role;
import uz.academixai.domain.User;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.infrastructure.security.TempPasswordGenerator;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §2.2 "O'qituvchilar" — admin invite/activate/deactivate/list over TEACHER users.
 */
@Service
public class TeacherManagementService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public TeacherManagementService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public List<User> list(UUID schoolId) {
    return userRepository
        .findByRoleAndSchoolIdOrderByLastNameAscFirstNameAsc(Role.TEACHER, schoolId)
        .stream()
        .map(UserEntity::toDomain)
        .toList();
  }

  // Two modes (DEVIATION on top of §2.2's password-less invite): when the admin supplies a
  // password, the account is created ACTIVE immediately — the admin is the credential-delivery
  // channel and hands the password to the teacher, who can change it later via
  // /auth/change-password. Without a password, the old behavior stands: server-generated temp
  // password + inactive until /activate (no delivery channel — known gap).
  public User invite(
      UUID schoolId,
      String phone,
      String firstName,
      String lastName,
      String email,
      String password) {
    boolean hasPassword = password != null && !password.isBlank();
    if (hasPassword && password.length() < 8) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_VALIDATION",
          "Parol kamida 8 belgidan iborat bo'lishi kerak.",
          "Uzunroq parol kiriting.");
    }
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
            Role.TEACHER,
            hasPassword,
            LocalDateTime.now(),
            null,
            schoolId);
    return userRepository.save(entity).toDomain();
  }

  public User activate(UUID schoolId, UUID teacherId) {
    return setActive(schoolId, teacherId, true);
  }

  public User deactivate(UUID schoolId, UUID teacherId) {
    return setActive(schoolId, teacherId, false);
  }

  private User setActive(UUID schoolId, UUID teacherId, boolean active) {
    UserEntity entity = requireOwned(schoolId, teacherId);
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

  private UserEntity requireOwned(UUID schoolId, UUID teacherId) {
    return userRepository
        .findByIdAndRoleAndSchoolId(teacherId, Role.TEACHER, schoolId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_TEACHER_NOT_FOUND",
                    "O'qituvchi topilmadi.",
                    "ID ni tekshiring yoki ro'yxatni yangilang."));
  }
}
