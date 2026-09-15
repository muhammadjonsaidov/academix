package uz.academixai.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.academixai.shared.tenancy.TenantScope;

/**
 * Establishes the requesting tenant's database scope for the whole request.
 *
 * <p>Runs after {@link JwtAuthenticationFilter} so the principal (and its {@code schoolId} claim)
 * is already populated, then hands the request to {@link TenantScope#runAsTenant}: one transaction
 * for the entire request, with {@code app.current_school_id}/{@code app.current_user_id} applied at
 * its start. Every repository call downstream joins that same transaction (Spring's default {@code
 * REQUIRED} propagation), so one scope covers the request — see CLAUDE.md "Backend architecture"
 * for why this is enforced at the database layer, not by app-level filtering.
 *
 * <p>Requests with no principal, or with a principal that has no school (an admin during initial
 * setup, before a school exists) are passed through unscoped. They can only reach endpoints that
 * never touch an RLS-scoped table; anything else fails loudly at the database rather than quietly
 * returning another tenant's rows.
 */
@Component
public class RlsTransactionFilter extends OncePerRequestFilter {

  private final TenantScope tenantScope;

  public RlsTransactionFilter(TenantScope tenantScope) {
    this.tenantScope = tenantScope;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws IOException, ServletException {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication != null
            && authentication.getPrincipal() instanceof AcademixPrincipal principal)
        || principal.schoolId() == null) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      tenantScope.runAsTenant(
          principal.schoolId(),
          principal.userId(),
          () -> {
            try {
              filterChain.doFilter(request, response);
            } catch (IOException | ServletException e) {
              throw new RlsFilterChainException(e);
            }
          });
    } catch (RlsFilterChainException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IOException io) {
        throw io;
      }
      if (cause instanceof ServletException se) {
        throw se;
      }
      throw e;
    }
  }

  private static final class RlsFilterChainException extends RuntimeException {
    RlsFilterChainException(Exception cause) {
      super(cause);
    }
  }
}
