package uz.academixai.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.identity.application.port.out.AccountRepository;
import uz.academixai.identity.domain.Account;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;

/** JPA adapter for the identity aggregate. */
@Repository
public class JpaAccountRepository implements AccountRepository {

  private final UserRepository users;

  public JpaAccountRepository(UserRepository users) {
    this.users = users;
  }

  @Override
  public Optional<Account> findByPhone(String phone) {
    return users.findByPhone(phone).map(JpaAccountRepository::toAccount);
  }

  @Override
  public Optional<Account> findByEmail(String email) {
    return users.findFirstByEmailIgnoreCase(email).map(JpaAccountRepository::toAccount);
  }

  @Override
  public Optional<Account> findById(UUID id) {
    return users.findById(id).map(JpaAccountRepository::toAccount);
  }

  @Override
  public long count() {
    return users.count();
  }

  @Override
  public Account save(Account account) {
    UserEntity entity =
        new UserEntity(
            account.id(),
            account.firstName(),
            account.lastName(),
            account.phone(),
            account.email(),
            account.passwordHash(),
            account.role(),
            account.active(),
            account.createdAt(),
            account.lastLoginAt(),
            account.schoolId());
    return toAccount(users.save(entity));
  }

  private static Account toAccount(UserEntity entity) {
    return new Account(
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
}
