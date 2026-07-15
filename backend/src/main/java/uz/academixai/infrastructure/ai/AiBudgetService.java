package uz.academixai.infrastructure.ai;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import uz.academixai.infrastructure.persistence.SchoolEntity;
import uz.academixai.infrastructure.persistence.SchoolRepository;

/**
 * academix_tz.md §8 — {@code monthlyAiCallLimit} splits into 3 independent, self-contained
 * sub-budgets so one category can't starve another (exam grading is protected — never eaten by a
 * spike in daily homework traffic). Real-time counting is Redis {@code INCR}, not Postgres — the
 * spec is explicit that per-call Postgres writes would cause row-lock contention under chat's
 * high-frequency traffic (300+ students, dozens of messages/day).
 *
 * <p><b>Scope note:</b> this only implements the budget gate itself ({@code isWithinAiBudget}/
 * {@code recordAiUsage}), which is what actually gates every AI call (task 22's grading consumer is
 * the first real caller). The {@code ai_usage_daily} Postgres dimensional breakdown ("which
 * class/subject/teacher spent the most") is admin-dashboard analytics, explicitly deferred to
 * whenever that dashboard gets built (see ROADMAP.md) — it's read-side reporting, not part of the
 * budget-gating mechanism itself.
 */
@Service
public class AiBudgetService {

  private static final Map<AiCallCategory, Double> BUDGET_SHARE =
      Map.of(
          AiCallCategory.EXAM, 0.15,
          AiCallCategory.HOMEWORK, 0.65,
          AiCallCategory.CHAT, 0.20);

  private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
  // A few days longer than the longest possible month so a key always outlives the month it
  // tracks, then gets cleaned up automatically rather than accumulating forever.
  private static final Duration KEY_TTL = Duration.ofDays(40);

  private final StringRedisTemplate redis;
  private final SchoolRepository schoolRepository;

  public AiBudgetService(StringRedisTemplate redis, SchoolRepository schoolRepository) {
    this.redis = redis;
    this.schoolRepository = schoolRepository;
  }

  public boolean isWithinAiBudget(UUID schoolId, AiCallCategory category) {
    int allocated = allocatedCalls(schoolId, category);
    long used = currentUsage(schoolId, category);
    return used < allocated;
  }

  public void recordAiUsage(UUID schoolId, AiCallCategory category) {
    String key = budgetKey(schoolId, category);
    Long newCount = redis.opsForValue().increment(key);
    if (newCount != null && newCount == 1L) {
      redis.expire(key, KEY_TTL);
    }
  }

  private int allocatedCalls(UUID schoolId, AiCallCategory category) {
    SchoolEntity school =
        schoolRepository
            .findById(schoolId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "AI budget check for unknown schoolId "
                            + schoolId
                            + " — JWT schoolId"
                            + " should always resolve to a real school row."));
    return (int) (school.getMonthlyAiCallLimit() * BUDGET_SHARE.get(category));
  }

  private long currentUsage(UUID schoolId, AiCallCategory category) {
    String value = redis.opsForValue().get(budgetKey(schoolId, category));
    return value == null ? 0L : Long.parseLong(value);
  }

  private String budgetKey(UUID schoolId, AiCallCategory category) {
    return "ai_budget_"
        + category.name().toLowerCase()
        + ":"
        + schoolId
        + ":"
        + YEAR_MONTH.format(LocalDate.now());
  }
}
