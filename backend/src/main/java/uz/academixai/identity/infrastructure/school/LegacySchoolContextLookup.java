package uz.academixai.identity.infrastructure.school;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.application.SchoolContextResolver;
import uz.academixai.identity.application.port.out.SchoolContextLookup;
import uz.academixai.identity.domain.Account;

/**
 * Transitional adapter while School's membership query is moved out of the legacy package.
 *
 * <p>The dependency points from an infrastructure adapter to the legacy implementation, never from
 * Identity's use case to it. It is replaced by School's published membership-query API during the
 * School context migration.
 */
@Component
public class LegacySchoolContextLookup implements SchoolContextLookup {

  private final SchoolContextResolver schoolContextResolver;

  public LegacySchoolContextLookup(SchoolContextResolver schoolContextResolver) {
    this.schoolContextResolver = schoolContextResolver;
  }

  @Override
  public Optional<UUID> resolve(Account account) {
    return schoolContextResolver.resolve(account.toUser());
  }
}
