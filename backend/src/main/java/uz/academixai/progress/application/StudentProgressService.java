package uz.academixai.progress.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.progress.application.port.in.ParentProgress.SubjectProgress;
import uz.academixai.progress.application.port.in.StudentDashboard;
import uz.academixai.progress.application.port.in.StudentDashboard.DashboardBadge;
import uz.academixai.progress.application.port.in.StudentDashboard.XpHistoryItem;
import uz.academixai.progress.application.port.out.GradeAverageLookup;

/**
 * academix_tz.md §2.4 {@code GET /student/progress} — exact response shape. {@code streakHistory}
 * is always empty: only current/max streak snapshots exist ({@code
 * student_profiles.current_streak}/{@code max_streak}), no historical log of streak changes over
 * time — a real, flagged gap, not a silently-dropped field.
 */
@Service
public class StudentProgressService {

  private final ParentProgressService parentProgressService;
  private final StudentDashboard studentDashboardService;
  private final GradeAverageLookup gradeAverages;

  public StudentProgressService(
      ParentProgressService parentProgressService,
      StudentDashboard studentDashboardService,
      GradeAverageLookup gradeAverages) {
    this.parentProgressService = parentProgressService;
    this.studentDashboardService = studentDashboardService;
    this.gradeAverages = gradeAverages;
  }

  public record Growth(double avgScore) {}

  public record MyGrowth(Growth thisMonth, Growth lastMonth, String growth) {}

  public record Progress(
      List<XpHistoryItem> xpHistory,
      List<SubjectProgress> subjectStats,
      List<DashboardBadge> badges,
      List<String> streakHistory,
      MyGrowth myGrowth) {}

  public Progress progress(UUID schoolId, UUID studentId) {
    List<XpHistoryItem> xpHistory = studentDashboardService.listXpHistory(schoolId, studentId);
    List<DashboardBadge> badges = studentDashboardService.listBadges(schoolId, studentId);
    List<SubjectProgress> subjectStats =
        parentProgressService.progressForStudent(studentId).subjectProgress();
    MyGrowth myGrowth = buildMyGrowth(studentId);

    return new Progress(xpHistory, subjectStats, badges, List.of(), myGrowth);
  }

  private MyGrowth buildMyGrowth(UUID studentId) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime thisMonthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
    LocalDateTime lastMonthStart = thisMonthStart.minusMonths(1);

    double thisMonthAvg = gradeAverages.averageScore(studentId, thisMonthStart, now);
    double lastMonthAvg = gradeAverages.averageScore(studentId, lastMonthStart, thisMonthStart);

    String growth;
    if (lastMonthAvg == 0) {
      growth = "N/A";
    } else {
      double changePercent = ((thisMonthAvg - lastMonthAvg) / lastMonthAvg) * 100;
      growth = (changePercent >= 0 ? "+" : "") + Math.round(changePercent) + "%";
    }

    return new MyGrowth(
        new Growth(Math.round(thisMonthAvg * 10) / 10.0),
        new Growth(Math.round(lastMonthAvg * 10) / 10.0),
        growth);
  }
}
