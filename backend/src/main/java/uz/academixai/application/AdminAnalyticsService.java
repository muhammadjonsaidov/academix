package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.infrastructure.persistence.GradeRepository.ClassProgressRow;
import uz.academixai.infrastructure.persistence.GradeRepository.PeriodProgressRow;
import uz.academixai.infrastructure.persistence.GradeRepository.TeacherRankingRow;

/**
 * academix_tz.md §2.2 {@code GET /admin/analytics/*} — "faqat admin ko'radi" (admin-only comparison
 * views). {@code period} (monthly|semester) has no exact window definition anywhere in the spec
 * beyond the enum values — judgment call: it controls the lookback window each query scores over
 * (monthly = last 30 days, semester = last ~6 months / half academic year), not a distinct
 * aggregation granularity. {@code school-progress}'s month-bucketed trend is unaffected by this
 * choice — see {@link GradeRepository#schoolProgress}'s Javadoc.
 */
@Service
public class AdminAnalyticsService {

  private static final int MONTHLY_WINDOW_DAYS = 30;
  private static final int SEMESTER_WINDOW_DAYS = 180;

  private final GradeRepository gradeRepository;

  public AdminAnalyticsService(GradeRepository gradeRepository) {
    this.gradeRepository = gradeRepository;
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

  private LocalDateTime windowSince(String period) {
    int days = "semester".equalsIgnoreCase(period) ? SEMESTER_WINDOW_DAYS : MONTHLY_WINDOW_DAYS;
    return LocalDateTime.now().minusDays(days);
  }
}
