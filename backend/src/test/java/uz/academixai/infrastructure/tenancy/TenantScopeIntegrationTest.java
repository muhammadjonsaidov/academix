package uz.academixai.infrastructure.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;
import uz.academixai.TestcontainersConfiguration;
import uz.academixai.shared.tenancy.TenantScope;

/**
 * Proves the tenancy mechanism end to end against real Postgres: that a tenant scope really sets
 * both settings for the whole unit of work, that it does not survive into the next transaction
 * (pooled connections are reused across requests, so a leaked setting would hand one school's data
 * to the next caller), and that the system scope really does bypass RLS — which is the only reason
 * the outbox publisher and the scheduled audits can see more than one tenant.
 *
 * <p>Runs the isolation assertions as a restricted, non-superuser role: Testcontainers connects as
 * a superuser, so a policy would otherwise be silently inert and the test would pass without
 * proving anything. Same discipline as {@code RlsMechanismTest}.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TenantScopeIntegrationTest {

  private static final String RESTRICTED_ROLE = "tenant_scope_test_role";
  private static final String PROBE_TABLE = "tenant_scope_probe";

  @Autowired private TenantScope tenantScope;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private EntityManager entityManager;

  private final UUID schoolA = UUID.randomUUID();
  private final UUID schoolB = UUID.randomUUID();
  private final UUID userA = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    transactionTemplate.executeWithoutResult(
        status -> {
          entityManager
              .createNativeQuery(
                  "DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '"
                      + RESTRICTED_ROLE
                      + "') THEN CREATE ROLE "
                      + RESTRICTED_ROLE
                      + " NOSUPERUSER NOBYPASSRLS NOINHERIT; END IF; END $$")
              .executeUpdate();
          entityManager.createNativeQuery("DROP TABLE IF EXISTS " + PROBE_TABLE).executeUpdate();
          entityManager
              .createNativeQuery(
                  "CREATE TABLE "
                      + PROBE_TABLE
                      + " (id uuid primary key default uuid_generate_v4(), school_id uuid not null)")
              .executeUpdate();
          entityManager
              .createNativeQuery("ALTER TABLE " + PROBE_TABLE + " ENABLE ROW LEVEL SECURITY")
              .executeUpdate();
          // Mirrors the policy shape every real migration uses — including its deliberate lack of
          // a default: an unset scope must raise, not quietly match nothing.
          entityManager
              .createNativeQuery(
                  "CREATE POLICY school_isolation ON "
                      + PROBE_TABLE
                      + " USING (school_id = current_setting('app.current_school_id')::uuid)")
              .executeUpdate();
          entityManager
              .createNativeQuery("GRANT USAGE ON SCHEMA public TO " + RESTRICTED_ROLE)
              .executeUpdate();
          entityManager
              .createNativeQuery("GRANT SELECT ON " + PROBE_TABLE + " TO " + RESTRICTED_ROLE)
              .executeUpdate();
          for (UUID school : List.of(schoolA, schoolB)) {
            entityManager
                .createNativeQuery("INSERT INTO " + PROBE_TABLE + " (school_id) VALUES (:schoolId)")
                .setParameter("schoolId", school)
                .executeUpdate();
          }
        });
  }

  @AfterEach
  void tearDown() {
    transactionTemplate.executeWithoutResult(
        status ->
            entityManager.createNativeQuery("DROP TABLE IF EXISTS " + PROBE_TABLE).executeUpdate());
  }

  @Test
  void tenantScopeSetsBothSettingsForTheWholeUnitOfWork() {
    List<String> seen = new ArrayList<>();

    tenantScope.runAsTenant(
        schoolA,
        userA,
        () -> {
          seen.add(currentSetting("app.current_school_id"));
          seen.add(currentSetting("app.current_user_id"));
        });

    assertThat(seen).containsExactly(schoolA.toString(), userA.toString());
  }

  /**
   * The property that matters is that the next unit of work on this (pooled) connection cannot see
   * the previous tenant — not that the setting is gone. Postgres keeps a placeholder for a custom
   * setting once it has been set, and reads back as an empty string rather than NULL afterwards,
   * which is exactly why unscoped queries in this project surface as {@code invalid input syntax
   * for type uuid: ""} instead of a missing-parameter error.
   */
  @Test
  void tenantScopeDoesNotLeakIntoTheNextTransaction() {
    tenantScope.runAsTenant(schoolA, userA, () -> {});

    assertThat(currentSettingOrNull("app.current_school_id"))
        .as("the next caller must never observe the previous tenant")
        .isNotEqualTo(schoolA.toString())
        .isNullOrEmpty();
    assertThat(currentSettingOrNull("app.current_user_id"))
        .isNotEqualTo(userA.toString())
        .isNullOrEmpty();
  }

  @Test
  void systemScopeRunsAsTheRestrictedBypassRoleAndSeesEveryTenant() {
    Long rows =
        tenantScope.callAsSystem(
            () -> {
              assertThat(currentUser())
                  .as("system work must enter the NOLOGIN role, not run as the app role")
                  .isEqualTo("academix_system");
              return countProbeRows();
            });

    assertThat(rows).as("the outbox publisher and scheduled audits depend on this").isEqualTo(2L);
  }

  @Test
  void restrictedRoleSeesOnlyItsOwnTenant() {
    Long visibleToSchoolA =
        runAsRestrictedRole(schoolA.toString(), "SELECT count(*) FROM " + PROBE_TABLE);

    assertThat(visibleToSchoolA).isEqualTo(1L);
  }

  @Test
  void restrictedRoleFailsLoudlyWithoutAScopeInsteadOfLeaking() {
    Exception failure =
        Assertions.assertThrows(
            Exception.class,
            () -> runAsRestrictedRole(null, "SELECT count(*) FROM " + PROBE_TABLE));

    assertThat(causesOf(failure))
        .as(
            "an unset scope must fail loudly instead of quietly matching nothing. The message is "
                + "either the missing setting's name or the uuid cast error Postgres raises for "
                + "the empty-string placeholder left behind by an earlier SET LOCAL")
        .anySatisfy(
            message ->
                assertThat(message)
                    .satisfiesAnyOf(
                        text -> assertThat(text).contains("app.current_school_id"),
                        text -> assertThat(text).contains("invalid input syntax for type uuid")));
  }

  /** Runs a query in a transaction whose only scope is the one given, as the restricted role. */
  private Long runAsRestrictedRole(String schoolId, String sql) {
    return transactionTemplate.execute(
        status -> {
          entityManager.createNativeQuery("SET LOCAL ROLE " + RESTRICTED_ROLE).executeUpdate();
          if (schoolId != null) {
            entityManager
                .createNativeQuery("SET LOCAL app.current_school_id = '" + schoolId + "'")
                .executeUpdate();
          }
          return ((Number) entityManager.createNativeQuery(sql).getSingleResult()).longValue();
        });
  }

  private Long countProbeRows() {
    return ((Number)
            entityManager
                .createNativeQuery("SELECT count(*) FROM " + PROBE_TABLE)
                .getSingleResult())
        .longValue();
  }

  private String currentSetting(String name) {
    return (String)
        entityManager.createNativeQuery("SELECT current_setting('" + name + "')").getSingleResult();
  }

  private String currentSettingOrNull(String name) {
    return (String)
        transactionTemplate.execute(
            status ->
                entityManager
                    .createNativeQuery("SELECT current_setting('" + name + "', true)")
                    .getSingleResult());
  }

  private String currentUser() {
    return (String) entityManager.createNativeQuery("SELECT current_user").getSingleResult();
  }

  private static List<String> causesOf(Throwable failure) {
    List<String> messages = new ArrayList<>();
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause.getMessage() != null) {
        messages.add(cause.getMessage());
      }
    }
    return messages;
  }
}
