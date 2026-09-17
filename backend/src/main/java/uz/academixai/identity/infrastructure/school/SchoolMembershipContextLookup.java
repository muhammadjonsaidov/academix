package uz.academixai.identity.infrastructure.school;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.identity.application.port.out.SchoolContextLookup;
import uz.academixai.identity.domain.Account;
import uz.academixai.school.application.port.in.SchoolMembership;
import uz.academixai.shared.tenancy.TenantScope;

/**
 * Identity's tenant claim, answered by School's published {@link SchoolMembership} API.
 *
 * <p>This replaces the transitional adapter that delegated into the legacy {@code
 * application.SchoolContextResolver} — the ordered plan's "school-context lookup" item, and the
 * reason Identity no longer depends on the legacy application package at all.
 *
 * <p><b>The system scope belongs here, not in School.</b> This runs while resolving a login, before
 * any tenant is known, so the membership read cannot be tenant-scoped; entering the system role is
 * the caller's decision, and keeping it in the pre-auth adapter is what stops a future
 * request-scoped caller from lifting row-level security for the rest of its transaction.
 */
@Component
public class SchoolMembershipContextLookup implements SchoolContextLookup {

  private final SchoolMembership membership;
  private final TenantScope tenantScope;

  public SchoolMembershipContextLookup(SchoolMembership membership, TenantScope tenantScope) {
    this.membership = membership;
    this.tenantScope = tenantScope;
  }

  @Override
  public Optional<UUID> resolve(Account account) {
    return tenantScope.callAsSystem(() -> membership.schoolOf(account.id(), account.role()));
  }
}
