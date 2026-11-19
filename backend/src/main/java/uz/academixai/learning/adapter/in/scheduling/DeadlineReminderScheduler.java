package uz.academixai.learning.adapter.in.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.academixai.learning.application.DeadlineReminderService;

/** Sends deadline reminders every afternoon, when students are home and can still do the work. */
@Component
public class DeadlineReminderScheduler {

  private static final Logger log = LoggerFactory.getLogger(DeadlineReminderScheduler.class);

  private final DeadlineReminderService reminders;

  public DeadlineReminderScheduler(DeadlineReminderService reminders) {
    this.reminders = reminders;
  }

  @Scheduled(cron = "0 0 16 * * *", zone = "Asia/Tashkent")
  public void remind() {
    log.info("Deadline reminders sent: {}", reminders.remindDueWithinADay());
  }
}
