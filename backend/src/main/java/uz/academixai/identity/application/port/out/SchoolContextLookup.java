package uz.academixai.identity.application.port.out;

import java.util.Optional;
import java.util.UUID;
import uz.academixai.identity.domain.Account;

/** Resolves the tenant claim needed by identity when it issues an access token. */
public interface SchoolContextLookup {

  Optional<UUID> resolve(Account account);
}
