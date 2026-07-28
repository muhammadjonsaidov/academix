package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.infrastructure.persistence.AiUsageLogRepository;
import uz.academixai.infrastructure.persistence.AiUsageLogRepository.ByClassRow;
import uz.academixai.infrastructure.persistence.AiUsageLogRepository.BySubjectRow;
import uz.academixai.infrastructure.persistence.AiUsageLogRepository.ByTeacherRow;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.infrastructure.persistence.GradeRepository.ClassProgressRow;
import uz.academixai.infrastructure.persistence.GradeRepository.PeriodProgressRow;
import uz.academixai.infrastructure.persistence.GradeRepository.TeacherRankingRow;

/**
 * academix_tz.md §2.2 {@code GET /admin/analytics/*} — "faqat admin ko'radi" (admin-only comparison
 * views). {@code period} (monthly|quarter) has no exact window definition anywhere in the spec
 * beyond the enum values — judgment call: it controls the lookback window each query scores over
 * (monthly = last 30 days, quarter = last ~90 days / 1/4 academic year), not a distinct aggregation
 * granularity. {@code school-progress}'s month-bucketed trend is unaffected by this choice — see
 * {@link GradeRepository#schoolProgress}'s Javadoc.
 */
@Service
public class AdminAnalyticsService {

  private static final int MONTHLY_WINDOW_DAYS = 30;
  private static final int QUARTER_WINDOW_DAYS = 90;

  private final GradeRepository gradeRepository;
  private final AiUsageLogRepository aiUsageLogRepository;

  public AdminAnalyticsService(
      GradeRepository gradeRepository, AiUsageLogRepository aiUsageLogRepository) {
    this.gradeRepository = gradeRepository;
    this.aiUsageLogRepository = aiUsageLogRepository;
  }

  public List<ClassProgressRow> classesComparison(UUID schoolId, UUID subjectId, String period) {
    return gradeRepository.classProgress(schoolId, subjectId, windowSince(period));
  }

  public List<TeacherRankingRow> teachersRanking(UUID schoolId) {
    return gradeRepository.teacherRanking(schoolId);
  }

  public List<PeriodProgressRow> schoolProgress(UUID schoolId, String period) {
    return gradeRepository.schoolProgress(schoolId, windowSince(period));
  }

  public record AiUsage(
      List<ByClassRow> byClass, List<BySubjectRow> bySubject, List<ByTeacherRow> byTeacher) {}

  // academix_tz.md §8 "Admin dashboard cost showback" — grading calls only (HOMEWORK/EXAM),
  // never CHAT, see ai_usage_log's migration comment.
  public AiUsage aiUsage(UUID schoolId, String period) {
    LocalDateTime since = windowSince(period);
    return new AiUsage(
        aiUsageLogRepository.usageByClass(schoolId, since),
        aiUsageLogRepository.usageBySubject(schoolId, since),
        aiUsageLogRepository.usageByTeacher(schoolId, since));
  }

  private LocalDateTime windowSince(String period) {
    int days = "quarter".equalsIgnoreCase(period) ? QUARTER_WINDOW_DAYS : MONTHLY_WINDOW_DAYS;
    return LocalDateTime.now().minusDays(days);
  }
}
