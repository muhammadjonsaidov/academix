package uz.academixai.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;
import uz.academixai.TestcontainersConfiguration;

/**
 * Proves the actual mechanism {@link RlsTransactionFilter} depends on: a {@code SET LOCAL
 * app.current_school_id} run inside a Spring-managed transaction is honored by Postgres RLS for
 * every query in that same transaction, and a different school_id sees a disjoint row set. Uses a
 * throwaway RLS-enabled table (real tables get this via migrations) so this doesn't depend on any
 * table existing yet.
 *
 * <p><b>Runs as a restricted role, not the test connection's default superuser.</b> Both the
 * local-dev Postgres user and Testcontainers' default user are superusers with BYPASSRLS —
 * confirmed by an earlier run of this test passing when it should have failed, because RLS is
 * silently skipped entirely for a superuser. {@code SET LOCAL ROLE} switches the effective role for
 * just this transaction (auto-resets at transaction end, no second connection/DataSource needed) so
 * these assertions reflect what the app's actual restricted runtime role ({@code academix_app}, see
 * infra/postgres-init/01-app-role.sh) will really experience.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RlsMechanismTest {

  private static final String RESTRICTED_ROLE = "rls_mechanism_test_role";

  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private EntityManager entityManager;

  private final UUID schoolA = UUID.randomUUID();
  private final UUID schoolB = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    transactionTemplate.executeWithoutResult(
        status -> {
          entityManager
              .createNativeQuery(
                  "DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname ="
                      + " 'rls_mechanism_test_role') THEN CREATE ROLE"
                      + " rls_mechanism_test_role NOSUPERUSER NOBYPASSRLS; END IF; END $$")
              .executeUpdate();
          entityManager
              .createNativeQuery(
                  "CREATE TABLE rls_mechanism_test (id uuid primary key default"
                      + " uuid_generate_v4(), school_id uuid not null, note text)")
              .executeUpdate();
          entityManager
              .createNativeQuery("ALTER TABLE rls_mechanism_test ENABLE ROW LEVEL SECURITY")
              .executeUpdate();
          entityManager
              .createNativeQuery(
                  "CREATE POLICY school_isolation ON rls_mechanism_test USING (school_id ="
                      + " current_setting('app.current_school_id')::uuid)")
              .executeUpdate();
          // RLS is an ADDITIONAL filter on top of normal grants, not a substitute for them —
          // the restricted role also needs an explicit table-level grant.
          entityManager
              .createNativeQuery("GRANT SELECT ON rls_mechanism_test TO rls_mechanism_test_role")
              .executeUpdate();
          entityManager
              .createNativeQuery(
                  "INSERT INTO rls_mechanism_test (school_id, note) VALUES (:a, 'from school A'),"
                      + " (:b, 'from school B')")
              .setParameter("a", schoolA)
              .setParameter("b", schoolB)
              .executeUpdate();
        });
  }

  @AfterEach
  void tearDown() {
    transactionTemplate.executeWithoutResult(
        status -> entityManager.createNativeQuery("DROP TABLE rls_mechanism_test").executeUpdate());
  }

  private void setLocalRoleAndSchool(UUID schoolId) {
    entityManager.createNativeQuery("SET LOCAL ROLE " + RESTRICTED_ROLE).executeUpdate();
    // SET LOCAL doesn't accept bind parameters (Postgres parses SET separately from normal
    // DML — a parameterized form is a syntax error, confirmed by a real failing test run).
    // Safe to inline directly: schoolId is a UUID object here, never raw user input.
    entityManager
        .createNativeQuery("SET LOCAL app.current_school_id = '" + schoolId + "'")
        .executeUpdate();
  }

  @Test
  @SuppressWarnings("unchecked")
  void schoolIdSetViaSetLocalOnlySeesItsOwnRows() {
    transactionTemplate.executeWithoutResult(
        status -> {
          setLocalRoleAndSchool(schoolA);

          List<Object[]> rows =
              entityManager
                  .createNativeQuery("SELECT id, school_id, note FROM rls_mechanism_test")
                  .getResultList();

          assertThat(rows).hasSize(1);
          assertThat(rows.get(0)[2]).isEqualTo("from school A");
        });
  }

  @Test
  @SuppressWarnings("unchecked")
  void differentSchoolIdSeesADisjointRowSet() {
    transactionTemplate.executeWithoutResult(
        status -> {
          setLocalRoleAndSchool(schoolB);

          List<Object[]> rows =
              entityManager
                  .createNativeQuery("SELECT id, school_id, note FROM rls_mechanism_test")
                  .getResultList();

          assertThat(rows).hasSize(1);
          assertThat(rows.get(0)[2]).isEqualTo("from school B");
        });
  }

  @Test
  void noSessionVariableSetFailsHardRatherThanLeaking() {
    // The real policy (matching production DDL exactly) uses single-arg current_setting(),
    // which fails hard if SET LOCAL was never run — never a silent empty/all-rows result.
    // The EXACT error text depends on whether this pooled connection has ever seen the
    // "app.current_school_id" name before (confirmed by a real test run): a connection's
    // first-ever reference raises "unrecognized configuration parameter"; on a connection
    // that's already used it at least once (the realistic case — HikariCP reuses
    // connections constantly), Postgres instead returns '' for the unset value and the
    // ::uuid cast raises "invalid input syntax for type uuid" instead. Both are hard
    // failures — that's the actual safety property, not the specific wording.
    assertThatThrownBy(
        () ->
            transactionTemplate.executeWithoutResult(
                status -> {
                  entityManager
                      .createNativeQuery("SET LOCAL ROLE " + RESTRICTED_ROLE)
                      .executeUpdate();
                  entityManager
                      .createNativeQuery("SELECT id, school_id, note FROM rls_mechanism_test")
                      .getResultList();
                }));
  }

  @Test
  void superuserConnectionBypassesRlsEntirely() {
    // Documents the exact footgun this whole file exists to catch: without SET LOCAL ROLE,
    // the test connection's default superuser sees ALL rows regardless of RLS policies or
    // app.current_school_id — proving why the app's runtime role must NOT be a superuser.
    transactionTemplate.executeWithoutResult(
        status -> {
          @SuppressWarnings("unchecked")
          List<Object[]> rows =
              entityManager
                  .createNativeQuery("SELECT id, school_id, note FROM rls_mechanism_test")
                  .getResultList();
          assertThat(rows).hasSize(2);
        });
  }
}
