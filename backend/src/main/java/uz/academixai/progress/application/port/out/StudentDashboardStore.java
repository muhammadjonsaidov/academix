package uz.academixai.progress.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.progress.domain.Badge;

/** Persistence read boundary for the student-owned Progress dashboard. */
public interface StudentDashboardStore {

  record Profile(String firstName, int totalXp, int currentStreak, int maxStreak) {}

  record AwardedBadge(Badge badge, LocalDateTime awardedAt) {}

  record Grade(UUID submissionId, int score, int fivePointGrade, LocalDateTime gradedAt) {}

  record XpEvent(LocalDateTime occurredAt, int xp, String reason) {}

  boolean userExists(UUID studentId);

  Optional<Profile> findProfile(UUID schoolId, UUID studentId);

  List<AwardedBadge> findAwardedBadges(UUID studentId);

  List<Grade> findRecentGrades(UUID studentId, int limit);

  List<XpEvent> findXpHistory(UUID studentId);

  List<Badge> findBadgeCatalog();
}
