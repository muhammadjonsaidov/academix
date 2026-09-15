package uz.academixai.family.infrastructure.legacy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uz.academixai.domain.Role;
import uz.academixai.family.application.port.out.ParentAccountStore;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.infrastructure.security.TempPasswordGenerator;

/**
 * Compatibility adapter: the parent account lives in Identity's {@code users} table, which Identity
 * still owns inside the legacy persistence package. Deleting this class is part of moving {@code
 * users} into Identity — until then it is the only place Family touches that table.
 *
 * <p>Writes go through the 11-argument {@link UserEntity} constructor so {@code school_id} survives
 * a rebuild; the 10-argument form silently nulls it and un-scopes the account (see CLAUDE.md).
 */
@Component
public class LegacyParentAccountStore implements ParentAccountStore {

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;

  public LegacyParentAccountStore(UserRepository users, PasswordEncoder passwordEncoder) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public Optional<ParentAccount> findByPhone(String phone) {
    return users.findByPhone(phone).map(LegacyParentAccountStore::toAccount);
  }

  @Override
  public boolean existsByPhone(String phone) {
    return users.existsByPhone(phone);
  }

  @Override
  public ParentAccount save(ParentAccount account) {
    return toAccount(users.save(toEntity(account)));
  }

  @Override
  public List<ParentAccount> findParentsOfSchool(UUID schoolId) {
    return users.findByRoleAndSchoolIdOrderByLastNameAscFirstNameAsc(Role.PARENT, schoolId).stream()
        .map(LegacyParentAccountStore::toAccount)
        .toList();
  }

  @Override
  public String encodePassword(String rawPassword) {
    return passwordEncoder.encode(rawPassword);
  }

  @Override
  public String generateTemporaryPassword() {
    return TempPasswordGenerator.generate();
  }

  private static ParentAccount toAccount(UserEntity entity) {
    return new ParentAccount(
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

  private static UserEntity toEntity(ParentAccount account) {
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
