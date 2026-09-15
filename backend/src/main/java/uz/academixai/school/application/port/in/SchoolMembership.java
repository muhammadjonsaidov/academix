package uz.academixai.school.application.port.in;

import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.Role;

/**
 * School's published answer to "which school does this user belong to" — the tenant claim Identity
 * puts into an access token, and the reason the ordered plan lists school-context lookup as
 * Identity's last remaining port.
 *
 * <p><b>Pre-authentication read.</b> It is called while resolving a login, i.e. before any tenant
 * is known, so the caller must run it in the system scope ({@code TenantScope.callAsSystem}) — it
 * must never be invoked from inside a request's tenant transaction, where entering the system role
 * would lift RLS for the rest of that request. The only caller today is Identity's {@code
 * SchoolContextLookup} adapter, which owns that scope.
 */
public interface SchoolMembership {

  Optional<UUID> schoolOf(UUID userId, Role role);
}
