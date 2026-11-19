package uz.academixai.intelligence.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uz.academixai.intelligence.application.port.out.AiUsageCounter;
import uz.academixai.intelligence.domain.AiCallCategory;

class AiBudgetServiceTest {

  @Test
  void warnsAdminsOnceWhenHomeworkUsageCrossesEightyPercent() {
    UUID schoolId = UUID.randomUUID();
    long[] used = {0};
    AiUsageCounter counter =
        new AiUsageCounter() {
          @Override
          public long currentUsage(UUID school, AiCallCategory category) {
            return used[0];
          }

          @Override
          public void increment(UUID school, AiCallCategory category) {
            used[0]++;
          }
        };
    List<Long> alertsAt = new ArrayList<>();
    // 100 calls a month → homework gets 65; 80% of that is 52.
    AiBudgetService budget =
        new AiBudgetService(
            school -> Optional.of(100),
            counter,
            (school, category, usedCalls, allocated) -> alertsAt.add(usedCalls));

    for (int i = 0; i < 65; i++) {
      budget.recordAiUsage(schoolId, AiCallCategory.HOMEWORK);
    }

    assertThat(alertsAt).containsExactly(52L);
    assertThat(budget.isWithinAiBudget(schoolId, AiCallCategory.HOMEWORK)).isFalse();
  }
}
