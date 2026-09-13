package uz.academixai.interfaces.web.student;

import java.util.List;
import uz.academixai.progress.application.port.in.StudentDashboard.Dashboard;

public record StudentDashboardResponse(
    ProfileSummary profile,
    List<BadgeResponse> badges,
    List<StudentHomeworkResponse> pendingHomework,
    List<RecentGradeResponse> recentGrades,
    int xpToNextBadge) {

  public record ProfileSummary(String firstName, int totalXp, int currentStreak, int maxStreak) {}

  public static StudentDashboardResponse from(Dashboard dashboard) {
    return new StudentDashboardResponse(
        new ProfileSummary(
            dashboard.firstName(),
            dashboard.totalXp(),
            dashboard.currentStreak(),
            dashboard.maxStreak()),
        dashboard.badges().stream().map(BadgeResponse::from).toList(),
        dashboard.pendingHomework().stream().map(StudentHomeworkResponse::from).toList(),
        dashboard.recentGrades().stream().map(RecentGradeResponse::from).toList(),
        dashboard.xpToNextBadge());
  }
}
