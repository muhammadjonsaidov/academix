package uz.academixai.progress.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.StudentProfile;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionEntity;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.progress.domain.Badge;
import uz.academixai.progress.domain.StudentBadge;
import uz.academixai.progress.domain.XpHistoryEntry;
import uz.academixai.progress.infrastructure.persistence.BadgeEntity;
import uz.academixai.progress.infrastructure.persistence.BadgeRepository;
import uz.academixai.progress.infrastructure.persistence.StudentBadgeEntity;
import uz.academixai.progress.infrastructure.persistence.StudentBadgeRepository;
import uz.academixai.progress.infrastructure.persistence.XpHistoryEntity;
import uz.academixai.progress.infrastructure.persistence.XpHistoryRepository;

/**
 * academix_tz.md §4 XPService. XP/streak only fire on AI_DONE/GRADED (anti-gaming, never on
 * SUBMITTED) — enforced by callers (AIAnalysisService, TeacherSubmissionService), not this class.
 *
 * <p><b>Idempotency judgment call (see ROADMAP.md Sprint 4):</b> the spec's own method signature
 * takes {@code submissionId}, not {@code studentId} — {@link #calculateAndAwardXP} uses that to
 * read/write {@code HomeworkSubmission.xpEarned} as the single source of truth per submission, so
 * calling it twice for the same submission (once at AI_DONE, again if a teacher later overrides the
 * score at GRADED) adjusts by the *delta* rather than double-awarding. This also makes "AI
 * natijasi, yoki teacher override qilsa teacher score" (TZ §4 XP table) work naturally: whichever
 * score is current when this runs is what counts.
 */
@Service
public class XPService {

  private static final int SEVEN_DAY_STREAK_BONUS = 50;
  private static final float STREAK_MIN_SCORE = 30f;

  private final HomeworkSubmissionRepository submissionRepository;
  private final StudentProfileRepository studentProfileRepository;
  private final XpHistoryRepository xpHistoryRepository;
  private final BadgeRepository badgeRepository;
  private final StudentBadgeRepository studentBadgeRepository;

  public XPService(
      HomeworkSubmissionRepository submissionRepository,
      StudentProfileRepository studentProfileRepository,
      XpHistoryRepository xpHistoryRepository,
      BadgeRepository badgeRepository,
      StudentBadgeRepository studentBadgeRepository) {
    this.submissionRepository = submissionRepository;
    this.studentProfileRepository = studentProfileRepository;
    this.xpHistoryRepository = xpHistoryRepository;
    this.badgeRepository = badgeRepository;
    this.studentBadgeRepository = studentBadgeRepository;
  }

  /** academix_tz.md §4 XP table — tiered by score, halved when late. */
  public void calculateAndAwardXP(UUID submissionId, float finalScorePercent, boolean isLate) {
    HomeworkSubmissionEntity subEntity =
        submissionRepository
            .findById(submissionId)
            .orElseThrow(() -> new IllegalStateException("Unknown submissionId " + submissionId));
    HomeworkSubmission submission = subEntity.toDomain();

    int newXp = tierXp(finalScorePercent, isLate);
    int previousXp = submission.xpEarned();
    int delta = newXp - previousXp;
    if (delta != 0) {
      addXp(
          submission.studentId(),
          delta,
          previousXp == 0 ? "Uy vazifasi baholandi" : "Ball qayta ko'rib chiqildi");
    }
    if (newXp != previousXp) {
      submissionRepository.save(
          HomeworkSubmissionEntity.fromDomain(withXpEarned(submission, newXp)));
    }
  }

  /** academix_tz.md §4 — +20 flat bonus, on top of the tier XP, when a teacher marks "A'lo". */
  public void awardExcellentBonus(UUID studentId) {
    addXp(studentId, 20, "O'qituvchi \"A'lo\" belgiladi");
  }

  /**
   * academix_tz.md §4 — day-based streak. Safe to call more than once per day for the same student
   * (e.g. once at AI_DONE, again at GRADED for the same submission): the same-day branch is a
   * no-op, so it never double-increments.
   */
  public void updateStreak(UUID studentId, float finalScorePercent) {
    StudentProfileEntity entity =
        studentProfileRepository
            .findByUserId(studentId)
            .orElseThrow(() -> new IllegalStateException("No student profile for " + studentId));
    StudentProfile profile = entity.toDomain();

    if (finalScorePercent < STREAK_MIN_SCORE) {
      studentProfileRepository.save(
          StudentProfileEntity.fromDomain(
              withStreak(profile, 0, profile.maxStreak(), profile.lastSubmissionDate())));
      return;
    }

    LocalDate today = LocalDate.now();
    LocalDate lastSubmissionDate = profile.lastSubmissionDate();
    if (today.equals(lastSubmissionDate)) {
      return;
    }

    int newStreak =
        lastSubmissionDate != null && lastSubmissionDate.equals(today.minusDays(1))
            ? profile.currentStreak() + 1
            : 1;
    int newMaxStreak = Math.max(profile.maxStreak(), newStreak);
    studentProfileRepository.save(
        StudentProfileEntity.fromDomain(withStreak(profile, newStreak, newMaxStreak, today)));

    if (newStreak % 7 == 0) {
      addXp(studentId, SEVEN_DAY_STREAK_BONUS, newStreak + " kunlik streak bonusi");
    }
  }

  /**
   * academix_tz.md §4 — awards any newly-qualifying badges, returns only the newly-awarded ones.
   */
  public List<Badge> checkAndAwardBadges(UUID studentId) {
    StudentProfile profile =
        studentProfileRepository
            .findByUserId(studentId)
            .map(StudentProfileEntity::toDomain)
            .orElseThrow(() -> new IllegalStateException("No student profile for " + studentId));

    List<Badge> newlyAwarded = new ArrayList<>();
    for (BadgeEntity badgeEntity : badgeRepository.findAll()) {
      Badge badge = badgeEntity.toDomain();
      if (studentBadgeRepository.existsByStudentIdAndBadgeId(studentId, badge.id())) {
        continue;
      }
      boolean qualifies =
          switch (badge.criteriaType()) {
            case TOTAL_XP -> profile.totalXp() >= badge.criteriaValue();
            case STREAK_DAYS -> profile.maxStreak() >= badge.criteriaValue();
          };
      if (qualifies) {
        StudentBadge awarded =
            new StudentBadge(UUID.randomUUID(), studentId, badge.id(), LocalDateTime.now());
        studentBadgeRepository.save(StudentBadgeEntity.fromDomain(awarded));
        newlyAwarded.add(badge);
      }
    }
    return newlyAwarded;
  }

  private void addXp(UUID studentId, int xp, String reason) {
    StudentProfileEntity entity =
        studentProfileRepository
            .findByUserId(studentId)
            .orElseThrow(() -> new IllegalStateException("No student profile for " + studentId));
    StudentProfile profile = entity.toDomain();
    studentProfileRepository.save(
        StudentProfileEntity.fromDomain(withTotalXp(profile, profile.totalXp() + xp)));

    XpHistoryEntry entry =
        new XpHistoryEntry(UUID.randomUUID(), studentId, xp, reason, LocalDateTime.now());
    xpHistoryRepository.save(XpHistoryEntity.fromDomain(entry));
  }

  private static int tierXp(float score, boolean isLate) {
    int tier;
    if (score <= 0) {
      tier = 0;
    } else if (score < 50) {
      tier = 5;
    } else if (score < 80) {
      tier = 10;
    } else if (score < 100) {
      tier = 15;
    } else {
      tier = 25;
    }
    return isLate ? tier / 2 : tier;
  }

  private static HomeworkSubmission withXpEarned(HomeworkSubmission submission, int xpEarned) {
    return new HomeworkSubmission(
        submission.id(),
        submission.schoolId(),
        submission.assignmentId(),
        submission.studentTaskId(),
        submission.studentId(),
        submission.type(),
        submission.textContent(),
        submission.imageUrl(),
        submission.status(),
        submission.isLate(),
        submission.submittedAt(),
        xpEarned);
  }

  private static StudentProfile withTotalXp(StudentProfile profile, int totalXp) {
    return new StudentProfile(
        profile.id(),
        profile.userId(),
        profile.classId(),
        profile.schoolId(),
        profile.studentNumber(),
        profile.birthDate(),
        totalXp,
        profile.currentStreak(),
        profile.maxStreak(),
        profile.lastSubmissionDate(),
        profile.isActive());
  }

  private static StudentProfile withStreak(
      StudentProfile profile, int currentStreak, int maxStreak, LocalDate lastSubmissionDate) {
    return new StudentProfile(
        profile.id(),
        profile.userId(),
        profile.classId(),
        profile.schoolId(),
        profile.studentNumber(),
        profile.birthDate(),
        profile.totalXp(),
        currentStreak,
        maxStreak,
        lastSubmissionDate,
        profile.isActive());
  }
}
