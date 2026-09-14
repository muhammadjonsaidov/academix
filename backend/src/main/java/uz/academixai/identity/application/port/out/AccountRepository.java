package uz.academixai.identity.application.port.out;

import java.util.Optional;
import java.util.UUID;
import uz.academixai.identity.domain.Account;

/** Persistence boundary owned by the identity context. */
public interface AccountRepository {

  Optional<Account> findByPhone(String phone);

  Optional<Account> findById(UUID id);

  /** Used only by the one-time product bootstrap. */
  long count();

  Account save(Account account);
}
