package uz.academixai.shared.tenancy;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * The single entry point for every database unit of work that needs a tenant or system scope.
 *
 * <p>Tenancy is enforced in the database (Postgres RLS policies read {@code
 * app.current_school_id}/{@code app.current_user_id}), not by application-level filtering. That
 * only works if every unit of work establishes its scope explicitly, so this port exists to make
 * the two legitimate scopes explicit and to keep raw {@code SET LOCAL} statements out of feature
 * code — a source-scanning test fails the build if they appear anywhere else.
 *
 * <p><b>Two scopes, and no third:</b>
 *
 * <ul>
 *   <li>{@link #runAsTenant}/{@link #callAsTenant} — normal request and consumer work. Opens a
 *       transaction, sets both GUCs, and lets RLS restrict every query to that tenant.
 *   <li>{@link #runAsSystem}/{@link #callAsSystem} — work that is cross-tenant by definition and
 *       cannot be scoped: the pre-authentication login lookup (no tenant is known yet), the outbox
 *       publisher, and scheduled system-wide audits. Enters the restricted NOLOGIN {@code
 *       academix_system} role, whose only privilege beyond {@code academix_app} is {@code
 *       BYPASSRLS}. Callers must state in a comment why the work cannot be tenant-scoped.
 * </ul>
 *
 * <p>Unscoped database access is not offered: without a scope, RLS policies raise rather than
 * silently return zero rows, so forgotten scope is a loud failure instead of a data leak.
 */
public interface TenantScope {

  /**
   * Runs {@code work} as the given tenant, in one transaction, with both scope settings applied.
   */
  void runAsTenant(UUID schoolId, UUID userId, Runnable work);

  /** {@link #runAsTenant} for work that returns a value. */
  <T> T callAsTenant(UUID schoolId, UUID userId, Supplier<T> work);

  /** Runs cross-tenant work as the system role. The caller owns the justification. */
  void runAsSystem(Runnable work);

  /** {@link #runAsSystem} for work that returns a value. */
  <T> T callAsSystem(Supplier<T> work);
}
