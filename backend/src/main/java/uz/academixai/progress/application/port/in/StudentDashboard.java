package uz.academixai.progress.application.port.in;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import uz.academixai.learning.application.port.in.StudentHomeworkQuery.HomeworkItem;
import uz.academixai.progress.domain.Badge;

/** Published Progress read API for the student dashboard, badges and XP history. */
public interface StudentDashboard {

  record DashboardBadge(Badge badge, LocalDateTime awardedAt) {}

  record RecentGrade(UUID submissionId, int score, int fivePointGrade, LocalDateTime gradedAt) {}

  record XpHistoryItem(LocalDateTime date, int xp, String reason) {}

  record Dashboard(
      String firstName,
      int totalXp,
      int currentStreak,
      int maxStreak,
      List<DashboardBadge> badges,
      List<HomeworkItem> pendingHomework,
      List<RecentGrade> recentGrades,
      int xpToNextBadge) {}

  Dashboard getDashboard(UUID schoolId, UUID studentId);

  List<DashboardBadge> listBadges(UUID schoolId, UUID studentId);

  List<XpHistoryItem> listXpHistory(UUID schoolId, UUID studentId);
}
