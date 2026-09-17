package uz.academixai.progress.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.Grade;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.learning.application.port.in.StudentHomeworkQuery;
import uz.academixai.progress.application.port.in.StudentDashboard;
import uz.academixai.progress.application.port.out.StudentDashboardStore;
import uz.academixai.progress.domain.Badge;
import uz.academixai.progress.domain.BadgeCriteriaType;

class StudentDashboardServiceTest {

  @Test
  void dashboardUsesOnlyNextUnearnedTotalXpBadge() {
    UUID schoolId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    Badge earned = badge(100);
    Badge next = badge(250);
    Store store = new Store(schoolId, studentId, List.of(earned, next));
    StudentDashboard dashboard = new StudentDashboardService(store, noHomework());

    StudentDashboard.Dashboard result = dashboard.getDashboard(schoolId, studentId);

    assertThat(result.xpToNextBadge()).isEqualTo(150);
    assertThat(result.badges())
        .singleElement()
        .extracting(item -> item.badge().id())
        .isEqualTo(earned.id());
  }

  @Test
  void dashboardRejectsProfileFromAnotherSchool() {
    Store store = new Store(UUID.randomUUID(), UUID.randomUUID(), List.of());
    StudentDashboard dashboard = new StudentDashboardService(store, noHomework());

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> dashboard.listBadges(UUID.randomUUID(), store.studentId))
        .hasMessageContaining("ruxsatingiz yo'q");
  }

  private static Badge badge(int threshold) {
    return new Badge(
        UUID.randomUUID(), "Badge " + threshold, "", "", BadgeCriteriaType.TOTAL_XP, threshold);
  }

  private static StudentHomeworkQuery noHomework() {
    return new StudentHomeworkQuery() {
      @Override
      public List<HomeworkItem> listHomework(UUID schoolId, UUID studentId) {
        return List.of();
      }

      @Override
      public HomeworkItem getHomeworkDetail(UUID schoolId, UUID studentId, UUID assignmentId) {
        return null;
      }

      @Override
      public List<HomeworkSubmission> listSubmissions(UUID schoolId, UUID studentId) {
        return List.of();
      }

      @Override
      public SubmissionDetail getSubmissionDetail(
          UUID schoolId, UUID studentId, UUID submissionId) {
        return new SubmissionDetail(null, (AIFeedback) null, (Grade) null);
      }
    };
  }

  private static final class Store implements StudentDashboardStore {

    private final UUID schoolId;
    private final UUID studentId;
    private final List<Badge> catalog;

    private Store(UUID schoolId, UUID studentId, List<Badge> catalog) {
      this.schoolId = schoolId;
      this.studentId = studentId;
      this.catalog = catalog;
    }

    @Override
    public boolean userExists(UUID studentId) {
      return this.studentId.equals(studentId);
    }

    @Override
    public Optional<Profile> findProfile(UUID schoolId, UUID studentId) {
      return this.schoolId.equals(schoolId) && this.studentId.equals(studentId)
          ? Optional.of(new Profile("Ali", 100, 3, 7))
          : Optional.empty();
    }

    @Override
    public List<AwardedBadge> findAwardedBadges(UUID studentId) {
      return catalog.isEmpty() || !this.studentId.equals(studentId)
          ? List.of()
          : List.of(new AwardedBadge(catalog.getFirst(), LocalDateTime.now()));
    }

    @Override
    public List<Grade> findRecentGrades(UUID studentId, int limit) {
      return List.of();
    }

    @Override
    public List<XpEvent> findXpHistory(UUID studentId) {
      return List.of();
    }

    @Override
    public List<Badge> findBadgeCatalog() {
      return catalog;
    }
  }
}
