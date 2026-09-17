package uz.academixai.reporting.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.reporting.application.port.out.AnalyticsStatistics;
import uz.academixai.reporting.application.port.out.LearningActivityCounts;
import uz.academixai.reporting.application.port.out.SchoolDirectoryCounts;
import uz.academixai.reporting.application.port.out.WellbeingAlertCounts;

/**
 * academix_tz.md §2.2 {@code GET /admin/dashboard} — exact response shape. {@code activeToday} and
 * {@code homeworkSubmissionRate} both use submission activity as the "active" proxy (see the legacy
 * query's own Javadoc for why); {@code classProgressList}/{@code teacherRankings} are scored over
 * the last 30 days of grades, a judgment call since the spec gives no window for the dashboard's
 * own summary lists (the dedicated {@code /admin/analytics/*} endpoints take an explicit {@code
 * period}).
 *
 * <p>Moved here from the legacy {@code application} package. It reached seven repositories; the
 * three count ports replace them, grouped by the data each set belongs to rather than by the table
 * it happens to sit in today.
 */
@Service
public class AdminDashboardService {

  private static final int SUBMISSION_RATE_WINDOW_DAYS = 30;
  private static final int DASHBOARD_SCORE_WINDOW_DAYS = 30;

  private final SchoolDirectoryCounts schoolCounts;
  private final LearningActivityCounts activity;
  private final WellbeingAlertCounts alerts;
  private final AnalyticsStatistics statistics;

  public AdminDashboardService(
      SchoolDirectoryCounts schoolCounts,
      LearningActivityCounts activity,
      WellbeingAlertCounts alerts,
      AnalyticsStatistics statistics) {
    this.schoolCounts = schoolCounts;
    this.activity = activity;
    this.alerts = alerts;
    this.statistics = statistics;
  }

  public record PsychAlertCounts(int high, int medium) {}

  public record Dashboard(
      int totalClasses,
      int totalStudents,
      int totalTeachers,
      int totalSubjects,
      int totalAssignments,
      int activeToday,
      double homeworkSubmissionRate,
      List<AnalyticsStatistics.ClassRow> classProgressList,
      List<AnalyticsStatistics.TeacherRow> teacherRankings,
      PsychAlertCounts psychologicalAlerts) {}

  public Dashboard dashboard(UUID schoolId) {
    SchoolDirectoryCounts.Counts counts = schoolCounts.of(schoolId);
    int totalStudents = counts.students();

    int totalAssignments = Math.toIntExact(activity.assignments(schoolId));
    int activeToday = activity.distinctSubmittersSince(schoolId, LocalDate.now().atStartOfDay());
    int activeInWindow =
        activity.distinctSubmittersSince(
            schoolId, LocalDateTime.now().minusDays(SUBMISSION_RATE_WINDOW_DAYS));
    double submissionRate = totalStudents == 0 ? 0.0 : (activeInWindow * 100.0) / totalStudents;

    LocalDateTime scoreWindowSince = LocalDateTime.now().minusDays(DASHBOARD_SCORE_WINDOW_DAYS);
    List<AnalyticsStatistics.ClassRow> classProgressList =
        statistics.classComparison(schoolId, null, scoreWindowSince);
    List<AnalyticsStatistics.TeacherRow> teacherRankings = statistics.teacherRanking(schoolId);

    PsychAlertCounts psychAlerts =
        new PsychAlertCounts(
            alerts.unresolved(schoolId, SignalSeverity.HIGH),
            alerts.unresolved(schoolId, SignalSeverity.MEDIUM));

    return new Dashboard(
        counts.classes(),
        totalStudents,
        counts.teachers(),
        counts.subjects(),
        totalAssignments,
        activeToday,
        Math.round(submissionRate * 10) / 10.0,
        classProgressList,
        teacherRankings,
        psychAlerts);
  }
}
