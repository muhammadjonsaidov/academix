package uz.academixai.infrastructure.tenancy;

import jakarta.persistence.EntityManager;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import uz.academixai.shared.tenancy.TenantScope;

/**
 * JPA/Postgres implementation of {@link TenantScope}.
 *
 * <p>Every method joins the caller's transaction when one exists and opens one otherwise (Spring's
 * default {@code REQUIRED} propagation), so {@code SET LOCAL} always lands on the same physical
 * transaction as the work it protects. {@code SET LOCAL} is transaction-scoped, so the setting
 * reverts when the connection returns to the pool — a pooled connection can never leak the previous
 * request's tenant into the next one.
 */
@Component
@EnableConfigurationProperties(TenantScopeProperties.class)
public class JpaTenantScope implements TenantScope {

  /** Postgres parses {@code SET} separately from DML, so values must be inlined, not bound. */
  private static final String SCHOOL_SETTING = "app.current_school_id";

  private static final String USER_SETTING = "app.current_user_id";

  /** A role name is interpolated into DDL, so it is validated rather than trusted. */
  private static final Pattern ROLE_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,62}");

  private final TransactionTemplate transactions;
  private final EntityManager entityManager;
  private final String systemRole;

  public JpaTenantScope(
      TransactionTemplate transactions,
      EntityManager entityManager,
      TenantScopeProperties properties) {
    this.transactions = transactions;
    this.entityManager = entityManager;
    this.systemRole = requireRoleName(properties.systemRole());
  }

  @Override
  public void runAsTenant(UUID schoolId, UUID userId, Runnable work) {
    callAsTenant(
        schoolId,
        userId,
        () -> {
          work.run();
          return null;
        });
  }

  @Override
  public <T> T callAsTenant(UUID schoolId, UUID userId, Supplier<T> work) {
    Objects.requireNonNull(schoolId, "A tenant scope needs a schoolId — use runAsSystem instead");
    return transactions.execute(
        status -> {
          applySetting(SCHOOL_SETTING, schoolId);
          if (userId != null) {
            applySetting(USER_SETTING, userId);
          }
          return work.get();
        });
  }

  @Override
  public void runAsSystem(Runnable work) {
    callAsSystem(
        () -> {
          work.run();
          return null;
        });
  }

  @Override
  public <T> T callAsSystem(Supplier<T> work) {
    return transactions.execute(
        status -> {
          entityManager.createNativeQuery("SET LOCAL ROLE " + systemRole).executeUpdate();
          return work.get();
        });
  }

  private void applySetting(String setting, UUID value) {
    // SET LOCAL does not accept JDBC bind parameters — a parameterized form is a Postgres syntax
    // error (confirmed by a real failing test). Safe to inline: value is a UUID object, so its
    // toString() is always the canonical hex-and-hyphens form.
    entityManager.createNativeQuery("SET LOCAL " + setting + " = '" + value + "'").executeUpdate();
  }

  private static String requireRoleName(String role) {
    if (!ROLE_NAME.matcher(role).matches()) {
      throw new IllegalStateException(
          "academix.db.system-role is not a valid Postgres role name: " + role);
    }
    return role;
  }
}
