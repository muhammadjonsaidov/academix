package uz.academixai.school.infrastructure.legacy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uz.academixai.domain.Role;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.infrastructure.security.TempPasswordGenerator;
import uz.academixai.school.application.port.out.MemberAccountStore;

/**
 * Compatibility adapter: the account row lives in Identity's {@code users} table, which Identity
 * still owns inside the legacy persistence package. Deleting this class is part of moving {@code
 * users} into Identity.
 */
@Component
public class LegacyMemberAccountStore implements MemberAccountStore {

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;

  public LegacyMemberAccountStore(UserRepository users, PasswordEncoder passwordEncoder) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public Optional<MemberAccount> findInSchool(UUID id, Role role, UUID schoolId) {
    return users
        .findByIdAndRoleAndSchoolId(id, role, schoolId)
        .map(LegacyMemberAccountStore::toAccount);
  }

  @Override
  public boolean existsByPhone(String phone) {
    return users.existsByPhone(phone);
  }

  @Override
  public List<MemberAccount> findByRoleInSchool(Role role, UUID schoolId) {
    return users.findByRoleAndSchoolIdOrderByLastNameAscFirstNameAsc(role, schoolId).stream()
        .map(LegacyMemberAccountStore::toAccount)
        .toList();
  }

  @Override
  public MemberAccount save(MemberAccount account) {
    return toAccount(users.save(toEntity(account)));
  }

  @Override
  public String encodePassword(String rawPassword) {
    return passwordEncoder.encode(rawPassword);
  }

  @Override
  public String generateTemporaryPassword() {
    return TempPasswordGenerator.generate();
  }

  private static MemberAccount toAccount(UserEntity entity) {
    return new MemberAccount(
        entity.getId(),
        entity.getFirstName(),
        entity.getLastName(),
        entity.getPhone(),
        entity.getEmail(),
        entity.getPasswordHash(),
        entity.getRole(),
        entity.isActive(),
        entity.getCreatedAt(),
        entity.getLastLoginAt(),
        entity.getSchoolId());
  }

  private static UserEntity toEntity(MemberAccount account) {
    return new UserEntity(
        account.id(),
        account.firstName(),
        account.lastName(),
        account.phone(),
        account.email(),
        account.passwordHash(),
        account.role(),
        account.isActive(),
        account.createdAt(),
        account.lastLoginAt(),
        account.schoolId());
  }
}
