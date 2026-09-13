package uz.academixai.progress.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.infrastructure.persistence.GradeEntity;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.progress.application.port.out.StudentDashboardStore;
import uz.academixai.progress.domain.Badge;

/** JPA adapter for Progress' student dashboard read data. */
@Repository
public class JpaStudentDashboardStore implements StudentDashboardStore {

  private final UserRepository users;
  private final StudentProfileRepository profiles;
  private final StudentBadgeRepository studentBadges;
  private final BadgeRepository badges;
  private final HomeworkSubmissionRepository submissions;
  private final GradeRepository grades;
  private final XpHistoryRepository xpHistory;

  public JpaStudentDashboardStore(
      UserRepository users,
      StudentProfileRepository profiles,
      StudentBadgeRepository studentBadges,
      BadgeRepository badges,
      HomeworkSubmissionRepository submissions,
      GradeRepository grades,
      XpHistoryRepository xpHistory) {
    this.users = users;
    this.profiles = profiles;
    this.studentBadges = studentBadges;
    this.badges = badges;
    this.submissions = submissions;
    this.grades = grades;
    this.xpHistory = xpHistory;
  }

  @Override
  public boolean userExists(UUID studentId) {
    return users.existsById(studentId);
  }

  @Override
  public Optional<Profile> findProfile(UUID schoolId, UUID studentId) {
    return profiles
        .findByUserIdAndSchoolId(studentId, schoolId)
        .map(StudentProfileEntity::toDomain)
        .flatMap(
            profile ->
                users
                    .findById(studentId)
                    .map(
                        user ->
                            new Profile(
                                user.getFirstName(),
                                profile.totalXp(),
                                profile.currentStreak(),
                                profile.maxStreak())));
  }

  @Override
  public List<AwardedBadge> findAwardedBadges(UUID studentId) {
    return studentBadges.findByStudentId(studentId).stream()
        .map(StudentBadgeEntity::toDomain)
        .map(
            awarded ->
                badges
                    .findById(awarded.badgeId())
                    .map(BadgeEntity::toDomain)
                    .map(badge -> new AwardedBadge(badge, awarded.awardedAt())))
        .flatMap(Optional::stream)
        .toList();
  }

  @Override
  public List<Grade> findRecentGrades(UUID studentId, int limit) {
    return submissions.findByStudentIdOrderBySubmittedAtDesc(studentId).stream()
        .filter(submission -> submission.toDomain().status() == SubmissionStatus.GRADED)
        .limit(limit)
        .map(submission -> grades.findBySubmissionId(submission.getId()))
        .flatMap(Optional::stream)
        .map(GradeEntity::toDomain)
        .map(
            grade ->
                new Grade(
                    grade.submissionId(), grade.score(), grade.fivePointGrade(), grade.gradedAt()))
        .toList();
  }

  @Override
  public List<XpEvent> findXpHistory(UUID studentId) {
    return xpHistory.findByStudentIdOrderByOccurredAtDesc(studentId).stream()
        .map(XpHistoryEntity::toDomain)
        .map(entry -> new XpEvent(entry.occurredAt(), entry.xp(), entry.reason()))
        .toList();
  }

  @Override
  public List<Badge> findBadgeCatalog() {
    return badges.findAll().stream().map(BadgeEntity::toDomain).toList();
  }
}
