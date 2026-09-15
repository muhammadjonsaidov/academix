package uz.academixai.identity.infrastructure.school;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.application.SchoolContextResolver;
import uz.academixai.identity.application.port.out.SchoolContextLookup;
import uz.academixai.identity.domain.Account;
import uz.academixai.shared.tenancy.TenantScope;

/**
 * Transitional adapter while School's membership query is moved out of the legacy package.
 *
 * <p>The dependency points from an infrastructure adapter to the legacy implementation, never from
 * Identity's use case to it. It is replaced by School's published membership-query API during the
 * School context migration.
 *
 * <p><b>Runs as the system role.</b> This resolves the tenant a login belongs to, which means it
 * runs before any tenant is known — the chicken-and-egg case {@link TenantScope#runAsSystem} exists
 * for. It must stay a narrow, indexed lookup ({@code users.phone}/{@code users.email} and {@code
 * schools.admin_id}); it is the only pre-authentication read of tenant tables in the app.
 */
@Component
public class LegacySchoolContextLookup implements SchoolContextLookup {

  private final SchoolContextResolver schoolContextResolver;
  private final TenantScope tenantScope;

  public LegacySchoolContextLookup(
      SchoolContextResolver schoolContextResolver, TenantScope tenantScope) {
    this.schoolContextResolver = schoolContextResolver;
    this.tenantScope = tenantScope;
  }

  @Override
  public Optional<UUID> resolve(Account account) {
    return tenantScope.callAsSystem(() -> schoolContextResolver.resolve(account.toUser()));
  }
}
