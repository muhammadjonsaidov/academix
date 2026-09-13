package uz.academixai.progress.application;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.interfaces.web.ApiException;
import uz.academixai.learning.application.port.in.StudentHomeworkQuery;
import uz.academixai.learning.application.port.in.StudentHomeworkQuery.HomeworkItem;
import uz.academixai.progress.application.port.in.StudentDashboard;
import uz.academixai.progress.application.port.out.StudentDashboardStore;
import uz.academixai.progress.domain.BadgeCriteriaType;

/** Progress-owned student dashboard read model, composed through persistence and Learning ports. */
@Service
public class StudentDashboardService implements StudentDashboard {

  private static final int RECENT_GRADES_LIMIT = 5;

  private final StudentDashboardStore store;
  private final StudentHomeworkQuery homework;

  public StudentDashboardService(StudentDashboardStore store, StudentHomeworkQuery homework) {
    this.store = store;
    this.homework = homework;
  }

  @Override
  public Dashboard getDashboard(UUID schoolId, UUID studentId) {
    StudentDashboardStore.Profile profile = requireProfile(schoolId, studentId);
    List<DashboardBadge> badges = listBadges(schoolId, studentId);
    List<HomeworkItem> pendingHomework =
        homework.listHomework(schoolId, studentId).stream()
            .filter(item -> "PENDING".equals(item.submissionStatus()))
            .toList();
    List<RecentGrade> recentGrades =
        store.findRecentGrades(studentId, RECENT_GRADES_LIMIT).stream()
            .map(
                grade ->
                    new RecentGrade(
                        grade.submissionId(),
                        grade.score(),
                        grade.fivePointGrade(),
                        grade.gradedAt()))
            .toList();
    return new Dashboard(
        profile.firstName(),
        profile.totalXp(),
        profile.currentStreak(),
        profile.maxStreak(),
        badges,
        pendingHomework,
        recentGrades,
        xpToNextBadge(profile.totalXp(), badges));
  }

  @Override
  public List<DashboardBadge> listBadges(UUID schoolId, UUID studentId) {
    requireProfile(schoolId, studentId);
    return store.findAwardedBadges(studentId).stream()
        .map(item -> new DashboardBadge(item.badge(), item.awardedAt()))
        .toList();
  }

  @Override
  public List<XpHistoryItem> listXpHistory(UUID schoolId, UUID studentId) {
    requireProfile(schoolId, studentId);
    return store.findXpHistory(studentId).stream()
        .map(item -> new XpHistoryItem(item.occurredAt(), item.xp(), item.reason()))
        .toList();
  }

  private StudentDashboardStore.Profile requireProfile(UUID schoolId, UUID studentId) {
    if (!store.userExists(studentId)) {
      throw new ApiException(
          HttpStatus.NOT_FOUND, "ERR_USER_NOT_FOUND", "Foydalanuvchi topilmadi.", "");
    }
    return store
        .findProfile(schoolId, studentId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ERR_ACCESS_DENIED",
                    "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
                    "O'quvchi profili topilmadi."));
  }

  private int xpToNextBadge(int totalXp, List<DashboardBadge> earnedBadges) {
    return store.findBadgeCatalog().stream()
        .filter(badge -> badge.criteriaType() == BadgeCriteriaType.TOTAL_XP)
        .filter(badge -> badge.criteriaValue() > totalXp)
        .filter(
            badge ->
                earnedBadges.stream().noneMatch(earned -> earned.badge().id().equals(badge.id())))
        .mapToInt(badge -> badge.criteriaValue() - totalXp)
        .min()
        .orElse(0);
  }
}
