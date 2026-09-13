package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.StudentProfile;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.infrastructure.persistence.GradeEntity;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.interfaces.web.ApiException;
import uz.academixai.learning.application.port.in.StudentHomeworkQuery;
import uz.academixai.learning.application.port.in.StudentHomeworkQuery.HomeworkItem;
import uz.academixai.progress.domain.Badge;
import uz.academixai.progress.domain.BadgeCriteriaType;
import uz.academixai.progress.infrastructure.persistence.BadgeEntity;
import uz.academixai.progress.infrastructure.persistence.BadgeRepository;
import uz.academixai.progress.infrastructure.persistence.StudentBadgeEntity;
import uz.academixai.progress.infrastructure.persistence.StudentBadgeRepository;
import uz.academixai.progress.infrastructure.persistence.XpHistoryEntity;
import uz.academixai.progress.infrastructure.persistence.XpHistoryRepository;

/** academix_tz.md §2.4 "GET /student/dashboard" — profile/badges/pendingHomework/recentGrades. */
@Service
public class StudentDashboardService {

  private static final int RECENT_GRADES_LIMIT = 5;

  private final UserRepository userRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final BadgeRepository badgeRepository;
  private final StudentBadgeRepository studentBadgeRepository;
  private final HomeworkSubmissionRepository submissionRepository;
  private final GradeRepository gradeRepository;
  private final StudentHomeworkQuery studentHomeworkQuery;
  private final XpHistoryRepository xpHistoryRepository;

  public StudentDashboardService(
      UserRepository userRepository,
      StudentProfileRepository studentProfileRepository,
      BadgeRepository badgeRepository,
      StudentBadgeRepository studentBadgeRepository,
      HomeworkSubmissionRepository submissionRepository,
      GradeRepository gradeRepository,
      StudentHomeworkQuery studentHomeworkQuery,
      XpHistoryRepository xpHistoryRepository) {
    this.userRepository = userRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.badgeRepository = badgeRepository;
    this.studentBadgeRepository = studentBadgeRepository;
    this.submissionRepository = submissionRepository;
    this.gradeRepository = gradeRepository;
    this.studentHomeworkQuery = studentHomeworkQuery;
    this.xpHistoryRepository = xpHistoryRepository;
  }

  public record DashboardBadge(Badge badge, LocalDateTime awardedAt) {}

  public record RecentGrade(
      UUID submissionId, int score, int fivePointGrade, LocalDateTime gradedAt) {}

  public record XpHistoryItem(LocalDateTime date, int xp, String reason) {}

  public record Dashboard(
      String firstName,
      int totalXp,
      int currentStreak,
      int maxStreak,
      List<DashboardBadge> badges,
      List<HomeworkItem> pendingHomework,
      List<RecentGrade> recentGrades,
      int xpToNextBadge) {}

  public Dashboard getDashboard(UUID schoolId, UUID studentId) {
    UserEntity user =
        userRepository
            .findById(studentId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_USER_NOT_FOUND",
                        "Foydalanuvchi topilmadi.",
                        ""));
    StudentProfile profile =
        studentProfileRepository
            .findByUserIdAndSchoolId(studentId, schoolId)
            .map(StudentProfileEntity::toDomain)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.FORBIDDEN,
                        "ERR_ACCESS_DENIED",
                        "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
                        "O'quvchi profili topilmadi."));

    List<DashboardBadge> badges =
        studentBadgeRepository.findByStudentId(studentId).stream()
            .map(StudentBadgeEntity::toDomain)
            .map(
                sb ->
                    badgeRepository
                        .findById(sb.badgeId())
                        .map(BadgeEntity::toDomain)
                        .map(b -> new DashboardBadge(b, sb.awardedAt())))
            .flatMap(Optional::stream)
            .toList();
    List<Badge> earnedBadges = badges.stream().map(DashboardBadge::badge).toList();

    List<HomeworkItem> pendingHomework =
        studentHomeworkQuery.listHomework(schoolId, studentId).stream()
            .filter(h -> "PENDING".equals(h.submissionStatus()))
            .toList();

    List<RecentGrade> recentGrades =
        submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
            .filter(s -> s.toDomain().status() == SubmissionStatus.GRADED)
            .limit(RECENT_GRADES_LIMIT)
            .map(s -> gradeRepository.findBySubmissionId(s.getId()))
            .flatMap(Optional::stream)
            .map(GradeEntity::toDomain)
            .map(
                g -> new RecentGrade(g.submissionId(), g.score(), g.fivePointGrade(), g.gradedAt()))
            .toList();

    int xpToNextBadge = computeXpToNextBadge(profile.totalXp(), earnedBadges);

    return new Dashboard(
        user.getFirstName(),
        profile.totalXp(),
        profile.currentStreak(),
        profile.maxStreak(),
        badges,
        pendingHomework,
        recentGrades,
        xpToNextBadge);
  }

  /** academix_tz.md §2.4 "GET /student/badges" — all badges this student has earned. */
  public List<DashboardBadge> listBadges(UUID schoolId, UUID studentId) {
    requireStudentProfile(schoolId, studentId);
    return studentBadgeRepository.findByStudentId(studentId).stream()
        .map(StudentBadgeEntity::toDomain)
        .map(
            sb ->
                badgeRepository
                    .findById(sb.badgeId())
                    .map(BadgeEntity::toDomain)
                    .map(b -> new DashboardBadge(b, sb.awardedAt())))
        .flatMap(Optional::stream)
        .toList();
  }

  /** academix_tz.md §2.4 "GET /student/xp-history" — {date, xp, reason} log, newest first. */
  public List<XpHistoryItem> listXpHistory(UUID schoolId, UUID studentId) {
    requireStudentProfile(schoolId, studentId);
    return xpHistoryRepository.findByStudentIdOrderByOccurredAtDesc(studentId).stream()
        .map(XpHistoryEntity::toDomain)
        .map(e -> new XpHistoryItem(e.occurredAt(), e.xp(), e.reason()))
        .toList();
  }

  private void requireStudentProfile(UUID schoolId, UUID studentId) {
    studentProfileRepository
        .findByUserIdAndSchoolId(studentId, schoolId)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ERR_ACCESS_DENIED",
                    "Ushbu ma'lumotni ko'rishga ruxsatingiz yo'q.",
                    "O'quvchi profili topilmadi."));
  }

  private int computeXpToNextBadge(int totalXp, List<Badge> earnedBadges) {
    return badgeRepository.findAll().stream()
        .map(BadgeEntity::toDomain)
        .filter(b -> b.criteriaType() == BadgeCriteriaType.TOTAL_XP)
        .filter(b -> b.criteriaValue() > totalXp)
        .filter(b -> earnedBadges.stream().noneMatch(e -> e.id().equals(b.id())))
        .mapToInt(b -> b.criteriaValue() - totalXp)
        .min()
        .orElse(0);
  }
}
