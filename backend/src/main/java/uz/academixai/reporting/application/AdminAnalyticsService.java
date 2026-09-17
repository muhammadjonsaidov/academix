package uz.academixai.reporting.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.reporting.application.port.out.AiUsageStatistics;
import uz.academixai.reporting.application.port.out.AnalyticsStatistics;

/**
 * academix_tz.md §2.2 {@code GET /admin/analytics/*} — "faqat admin ko'radi" (admin-only comparison
 * views). {@code period} (monthly|quarter) has no exact window definition anywhere in the spec
 * beyond the enum values — judgment call: it controls the lookback window each query scores over
 * (monthly = last 30 days, quarter = last ~90 days / 1/4 academic year), not a distinct aggregation
 * granularity. {@code school-progress}'s month-bucketed trend is unaffected by this choice — the
 * bucketing lives in the query behind {@link AnalyticsStatistics#schoolProgress}.
 *
 * <p>Moved here from the legacy {@code application} package: the window policy is a Reporting
 * decision, and the rows it returns are Reporting's own records rather than the persistence
 * projections of whoever happened to own the tables.
 */
@Service
public class AdminAnalyticsService {

  private static final int MONTHLY_WINDOW_DAYS = 30;
  private static final int QUARTER_WINDOW_DAYS = 90;

  private final AnalyticsStatistics statistics;
  private final AiUsageStatistics aiUsage;

  public AdminAnalyticsService(AnalyticsStatistics statistics, AiUsageStatistics aiUsage) {
    this.statistics = statistics;
    this.aiUsage = aiUsage;
  }

  public List<AnalyticsStatistics.ClassRow> classesComparison(
      UUID schoolId, UUID subjectId, String period) {
    return statistics.classComparison(schoolId, subjectId, windowSince(period));
  }

  public List<AnalyticsStatistics.TeacherRow> teachersRanking(UUID schoolId) {
    return statistics.teacherRanking(schoolId);
  }

  public List<AnalyticsStatistics.PeriodRow> schoolProgress(UUID schoolId, String period) {
    return statistics.schoolProgress(schoolId, windowSince(period));
  }

  public record AiUsage(
      List<AiUsageStatistics.ByClass> byClass,
      List<AiUsageStatistics.BySubject> bySubject,
      List<AiUsageStatistics.ByTeacher> byTeacher) {}

  // academix_tz.md §8 "Admin dashboard cost showback" — grading calls only (HOMEWORK/EXAM),
  // never CHAT, see ai_usage_log's migration comment.
  public AiUsage aiUsage(UUID schoolId, String period) {
    LocalDateTime since = windowSince(period);
    return new AiUsage(
        aiUsage.byClass(schoolId, since),
        aiUsage.bySubject(schoolId, since),
        aiUsage.byTeacher(schoolId, since));
  }

  private LocalDateTime windowSince(String period) {
    int days = "quarter".equalsIgnoreCase(period) ? QUARTER_WINDOW_DAYS : MONTHLY_WINDOW_DAYS;
    return LocalDateTime.now().minusDays(days);
  }
}
