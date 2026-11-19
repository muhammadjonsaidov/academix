package uz.academixai.intelligence.application;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.intelligence.application.port.out.AiBudgetAlerts;
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

  /** Share of a sub-budget after which the school's admins are warned (once per month). */
  static final double LOW_BUDGET_THRESHOLD = 0.8;

  private final AiMonthlyLimitLookup limits;
  private final AiUsageCounter usage;
  private final AiBudgetAlerts alerts;

  public AiBudgetService(AiMonthlyLimitLookup limits, AiUsageCounter usage, AiBudgetAlerts alerts) {
    this.limits = limits;
    this.usage = usage;
    this.alerts = alerts;
  }

  public boolean isWithinAiBudget(UUID schoolId, AiCallCategory category) {
    return usage.currentUsage(schoolId, category) < allocatedCalls(schoolId, category);
  }

  /** Used by exam creation to disclose the current pre-flight capacity. */
  public int remainingBudget(UUID schoolId, AiCallCategory category) {
    return (int)
        Math.max(0, allocatedCalls(schoolId, category) - usage.currentUsage(schoolId, category));
  }

  /**
   * Counts one AI call. The call that crosses 80% of the sub-budget warns the admins; the counter
   * resets monthly, so that warning goes out at most once a month per category.
   */
  public void recordAiUsage(UUID schoolId, AiCallCategory category) {
    long before = usage.currentUsage(schoolId, category);
    usage.increment(schoolId, category);
    int allocated = allocatedCalls(schoolId, category);
    long threshold = (long) Math.ceil(allocated * LOW_BUDGET_THRESHOLD);
    if (allocated > 0 && before < threshold && before + 1 >= threshold) {
      alerts.budgetRunningLow(schoolId, category, before + 1, allocated);
    }
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
