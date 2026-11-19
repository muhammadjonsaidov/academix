package uz.academixai.intelligence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;
import uz.academixai.TestcontainersConfiguration;
import uz.academixai.intelligence.domain.AiCallCategory;

/**
 * Real Postgres (school row, monthlyAiCallLimit) + real Redis (INCR-based usage counter) — no
 * mocks, matching this project's "infra-touching code gets a real Testcontainers test" discipline
 * (see write-integration-test skill / RlsMechanismTest).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AiBudgetServiceTest {

  @Autowired private AiBudgetService aiBudgetService;
  @Autowired private EntityManager entityManager;
  @Autowired private TransactionTemplate transactionTemplate;

  private UUID schoolId;

  // 1000 -> exam=150 (15%), homework=650 (65%), chat=200 (20%) — clean round numbers to assert
  // against exactly.
  private static final int MONTHLY_LIMIT = 1000;

  // Real commit via TransactionTemplate, not @Transactional on the test method — Spring's test
  // framework auto-rolls back @Transactional test methods, which would silently delete this row
  // before the test body (running in its own separate connection/transaction) ever saw it. Same
  // reasoning as RlsMechanismTest.
  @BeforeEach
  void createSchoolWithKnownBudget() {
    schoolId = UUID.randomUUID();
    transactionTemplate.executeWithoutResult(
        status ->
            entityManager
                .createNativeQuery(
                    "INSERT INTO schools (id, name, address, region, district,"
                        + " monthly_ai_call_limit) VALUES (:id, 'Budget Test School', 'addr',"
                        + " 'region', 'district', :limit)")
                .setParameter("id", schoolId)
                .setParameter("limit", MONTHLY_LIMIT)
                .executeUpdate());
  }

  @AfterEach
  void deleteSchool() {
    transactionTemplate.executeWithoutResult(
        status ->
            entityManager
                .createNativeQuery("DELETE FROM schools WHERE id = :id")
                .setParameter("id", schoolId)
                .executeUpdate());
  }

  @Test
  void withinBudgetBeforeAnyUsage() {
    assertThat(aiBudgetService.isWithinAiBudget(schoolId, AiCallCategory.HOMEWORK)).isTrue();
    assertThat(aiBudgetService.isWithinAiBudget(schoolId, AiCallCategory.EXAM)).isTrue();
    assertThat(aiBudgetService.isWithinAiBudget(schoolId, AiCallCategory.CHAT)).isTrue();
  }

  @Test
  void recordAiUsageIncrementsAndEventuallyExhaustsOneCategoryOnly() {
    // CHAT gets 20% of 1000 = 200. Use exactly that many calls.
    for (int i = 0; i < 200; i++) {
      assertThat(aiBudgetService.isWithinAiBudget(schoolId, AiCallCategory.CHAT))
          .as("call #%d should still be within budget", i)
          .isTrue();
      aiBudgetService.recordAiUsage(schoolId, AiCallCategory.CHAT);
    }

    assertThat(aiBudgetService.isWithinAiBudget(schoolId, AiCallCategory.CHAT))
        .as("201st chat call should be over budget")
        .isFalse();

    // Independent sub-budgets: chat exhaustion must not touch homework or exam.
    assertThat(aiBudgetService.isWithinAiBudget(schoolId, AiCallCategory.HOMEWORK)).isTrue();
    assertThat(aiBudgetService.isWithinAiBudget(schoolId, AiCallCategory.EXAM)).isTrue();
  }

  @Test
  void unknownSchoolFailsFastRatherThanSilentlyAllowing() {
    assertThatThrownBy(
            () -> aiBudgetService.isWithinAiBudget(UUID.randomUUID(), AiCallCategory.HOMEWORK))
        .isInstanceOf(IllegalStateException.class);
  }
}
