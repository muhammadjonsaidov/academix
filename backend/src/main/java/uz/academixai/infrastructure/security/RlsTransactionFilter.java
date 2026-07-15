package uz.academixai.infrastructure.security;

import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Wraps the whole request in one transaction and runs {@code SET LOCAL app.current_school_id} once
 * at its start — every downstream repository call joins that same transaction (Spring's default
 * REQUIRED propagation), so one SET LOCAL covers the whole request. See CLAUDE.md "Backend
 * architecture" for why this is enforced at the DB layer, not app-level filtering.
 *
 * <p>Runs after {@link JwtAuthenticationFilter} so the principal is already populated. Skips
 * entirely when there's no authenticated principal or no resolved schoolId (anonymous requests, and
 * — for now — TEACHER/STUDENT/PARENT, whose schoolId resolution isn't implemented yet; see {@code
 * SchoolContextResolver}). Sprint 1 doesn't touch any RLS-scoped table, so this is safe; must be
 * revisited before the Homework epic ships.
 */
@Component
public class RlsTransactionFilter extends OncePerRequestFilter {

  private final TransactionTemplate transactionTemplate;
  private final EntityManager entityManager;

  public RlsTransactionFilter(
      TransactionTemplate transactionTemplate, EntityManager entityManager) {
    this.transactionTemplate = transactionTemplate;
    this.entityManager = entityManager;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication != null
            && authentication.getPrincipal() instanceof AcademixPrincipal principal)
        || principal.schoolId() == null) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      transactionTemplate.executeWithoutResult(
          status -> {
            // SET LOCAL does not accept JDBC bind parameters (Postgres parses SET commands
            // separately from normal DML) — a parameterized "SET LOCAL ... = :x" is a syntax
            // error, confirmed by a real failing test. Safe to inline directly: schoolId is a
            // UUID object here, not raw user input, so its toString() is always the canonical
            // hex-and-hyphens form with no characters that could break out of the literal.
            entityManager
                .createNativeQuery(
                    "SET LOCAL app.current_school_id = '" + principal.schoolId() + "'")
                .executeUpdate();
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
