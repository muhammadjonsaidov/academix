package uz.academixai.intelligence.application;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.intelligence.application.port.out.AiMonthlyLimitLookup;
import uz.academixai.intelligence.application.port.out.AiUsageCounter;
import uz.academixai.intelligence.domain.AiCallCategory;

/** Application policy for a school's independent AI monthly sub-budgets. */
@Service
public class AiBudgetService {

  private static final Map<AiCallCategory, Double> BUDGET_SHARE =
      Map.of(
          AiCallCategory.EXAM, 0.15,
          AiCallCategory.HOMEWORK, 0.65,
          AiCallCategory.CHAT, 0.20);

  private final AiMonthlyLimitLookup limits;
  private final AiUsageCounter usage;

  public AiBudgetService(AiMonthlyLimitLookup limits, AiUsageCounter usage) {
    this.limits = limits;
    this.usage = usage;
  }

  public boolean isWithinAiBudget(UUID schoolId, AiCallCategory category) {
    return usage.currentUsage(schoolId, category) < allocatedCalls(schoolId, category);
  }

  /** Used by exam creation to disclose the current pre-flight capacity. */
  public int remainingBudget(UUID schoolId, AiCallCategory category) {
    return (int)
        Math.max(0, allocatedCalls(schoolId, category) - usage.currentUsage(schoolId, category));
  }

  public void recordAiUsage(UUID schoolId, AiCallCategory category) {
    usage.increment(schoolId, category);
  }

  private int allocatedCalls(UUID schoolId, AiCallCategory category) {
    int monthlyLimit =
        limits
            .monthlyLimit(schoolId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "AI budget check for unknown schoolId "
                            + schoolId
                            + " — JWT schoolId should resolve to a real school row."));
    return (int) (monthlyLimit * BUDGET_SHARE.get(category));
  }
}
