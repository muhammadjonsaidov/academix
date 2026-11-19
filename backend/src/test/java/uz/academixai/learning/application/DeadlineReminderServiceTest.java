package uz.academixai.learning.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uz.academixai.learning.application.port.out.DueHomeworkLookup.DueHomework;
import uz.academixai.learning.application.port.out.LearningNotifier;

class DeadlineReminderServiceTest {

  private static final ZoneId TASHKENT = ZoneId.of("Asia/Tashkent");

  @Test
  void remindsEveryStudentWhoStillOwesWorkDueWithinTheNextDay() {
    Clock clock = Clock.fixed(Instant.parse("2026-11-19T11:00:00Z"), TASHKENT);
    LocalDateTime now = LocalDateTime.now(clock);
    List<LocalDateTime[]> windows = new ArrayList<>();
    List<UUID> reminded = new ArrayList<>();
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();

    DeadlineReminderService service =
        new DeadlineReminderService(
            (from, to) -> {
              windows.add(new LocalDateTime[] {from, to});
              return List.of(
                  new DueHomework(first, UUID.randomUUID(), "Kasrlar", now.plusHours(5)),
                  new DueHomework(second, UUID.randomUUID(), "Insho", now.plusHours(20)));
            },
            new RecordingNotifier(reminded),
            clock);

    assertThat(service.remindDueWithinADay()).isEqualTo(2);
    assertThat(reminded).containsExactly(first, second);
    assertThat(windows)
        .singleElement()
        .satisfies(
            window -> {
              assertThat(window[0]).isEqualTo(now);
              assertThat(window[1]).isEqualTo(now.plusDays(1));
            });
  }

  private record RecordingNotifier(List<UUID> reminded) implements LearningNotifier {
    @Override
    public void homeworkAssigned(
        UUID schoolId, UUID classId, UUID assignmentId, String title, LocalDateTime deadlineAt) {}

    @Override
    public void homeworkGraded(
        UUID studentId,
        UUID submissionId,
        String assignmentTitle,
        int score,
        int fivePointGrade,
        boolean regraded) {}

    @Override
    public void deadlineApproaching(
        UUID studentId, UUID assignmentId, String title, LocalDateTime deadlineAt) {
      reminded.add(studentId);
    }
  }
}
