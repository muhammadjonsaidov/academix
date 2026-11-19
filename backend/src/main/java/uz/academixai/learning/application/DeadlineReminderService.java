package uz.academixai.learning.application;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import uz.academixai.learning.application.port.out.DueHomeworkLookup;
import uz.academixai.learning.application.port.out.LearningNotifier;

/**
 * Daily reminder for students who have not submitted work that is due within the next day. Runs
 * once a day over a 24-hour window, so each assignment is reminded about at most once.
 */
@Service
public class DeadlineReminderService {

  private final DueHomeworkLookup dueHomework;
  private final LearningNotifier notifier;
  private final Clock clock;

  public DeadlineReminderService(
      DueHomeworkLookup dueHomework, LearningNotifier notifier, Clock clock) {
    this.dueHomework = dueHomework;
    this.notifier = notifier;
    this.clock = clock;
  }

  /** Returns how many reminders were sent. */
  public int remindDueWithinADay() {
    LocalDateTime now = LocalDateTime.now(clock);
    var due = dueHomework.unsubmittedWithDeadlineBetween(now, now.plusDays(1));
    due.forEach(
        item ->
            notifier.deadlineApproaching(
                item.studentId(), item.assignmentId(), item.title(), item.deadlineAt()));
    return due.size();
  }
}
