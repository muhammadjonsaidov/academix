package uz.academixai.progress.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.StudentProfile;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.domain.SubmissionType;
import uz.academixai.progress.application.port.out.HomeworkSubmissionXpStore;
import uz.academixai.progress.application.port.out.StudentBadgeStore;
import uz.academixai.progress.application.port.out.StudentProgressProfileStore;
import uz.academixai.progress.application.port.out.XpHistoryStore;
import uz.academixai.progress.domain.StudentBadge;
import uz.academixai.progress.domain.XpHistoryEntry;

class XPServiceTest {

  private final UUID schoolId = UUID.randomUUID();
  private final UUID studentId = UUID.randomUUID();
  private final UUID submissionId = UUID.randomUUID();
  private final Submissions submissions = new Submissions();
  private final Profiles profiles = new Profiles();
  private final History history = new History();
  private XPService service;

  @BeforeEach
  void setUp() {
    submissions.save(
        new HomeworkSubmission(
            submissionId,
            schoolId,
            UUID.randomUUID(),
            null,
            studentId,
            SubmissionType.TEXT,
            "answer",
            null,
            SubmissionStatus.AI_DONE,
            false,
            LocalDateTime.now(),
            0));
    profiles.save(
        new StudentProfile(
            UUID.randomUUID(),
            studentId,
            UUID.randomUUID(),
            schoolId,
            "S-1",
            null,
            0,
            0,
            0,
            null,
            true));
    service = new XPService(submissions, profiles, history, List::of, new StudentBadges());
  }

  @Test
  void adjustsOnlyTheXpDeltaWhenScoreIsOverridden() {
    service.calculateAndAwardXP(submissionId, 85, false);
    service.calculateAndAwardXP(submissionId, 100, false);

    assertThat(submissions.findById(submissionId).orElseThrow().xpEarned()).isEqualTo(25);
    assertThat(profiles.findByStudentId(studentId).orElseThrow().totalXp()).isEqualTo(25);
    assertThat(history.entries()).extracting(XpHistoryEntry::xp).containsExactly(15, 10);
  }

  @Test
  void resetsStreakForAScoreBelowThreshold() {
    profiles.save(
        new StudentProfile(
            UUID.randomUUID(),
            studentId,
            UUID.randomUUID(),
            schoolId,
            "S-1",
            null,
            0,
            4,
            8,
            LocalDate.now().minusDays(1),
            true));

    service.updateStreak(studentId, 0);

    StudentProfile updated = profiles.findByStudentId(studentId).orElseThrow();
    assertThat(updated.currentStreak()).isZero();
    assertThat(updated.maxStreak()).isEqualTo(8);
  }

  private static final class Submissions implements HomeworkSubmissionXpStore {

    private final Map<UUID, HomeworkSubmission> values = new HashMap<>();

    @Override
    public Optional<HomeworkSubmission> findById(UUID submissionId) {
      return Optional.ofNullable(values.get(submissionId));
    }

    @Override
    public HomeworkSubmission save(HomeworkSubmission submission) {
      values.put(submission.id(), submission);
      return submission;
    }
  }

  private static final class Profiles implements StudentProgressProfileStore {

    private final Map<UUID, StudentProfile> values = new HashMap<>();

    @Override
    public Optional<StudentProfile> findByStudentId(UUID studentId) {
      return Optional.ofNullable(values.get(studentId));
    }

    @Override
    public StudentProfile save(StudentProfile profile) {
      values.put(profile.userId(), profile);
      return profile;
    }
  }

  private static final class History implements XpHistoryStore {

    private final List<XpHistoryEntry> values = new ArrayList<>();

    @Override
    public XpHistoryEntry save(XpHistoryEntry entry) {
      values.add(entry);
      return entry;
    }

    List<XpHistoryEntry> entries() {
      return values;
    }
  }

  private static final class StudentBadges implements StudentBadgeStore {

    @Override
    public boolean existsByStudentIdAndBadgeId(UUID studentId, UUID badgeId) {
      return false;
    }

    @Override
    public StudentBadge save(StudentBadge badge) {
      return badge;
    }
  }
}
