package uz.academixai.learning.adapter.in.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.academixai.learning.application.StuckSubmissionService;

/** Every ten minutes, hands homework the AI never finished back to the teacher. */
@Component
public class StuckSubmissionScheduler {

  private static final Logger log = LoggerFactory.getLogger(StuckSubmissionScheduler.class);

  private final StuckSubmissionService stuckSubmissions;

  public StuckSubmissionScheduler(StuckSubmissionService stuckSubmissions) {
    this.stuckSubmissions = stuckSubmissions;
  }

  @Scheduled(fixedDelay = 600_000, initialDelay = 600_000)
  public void release() {
    int released = stuckSubmissions.releaseStuck();
    if (released > 0) {
      log.warn(
          "Released {} homework submission(s) the AI never finished to manual grading", released);
    }
  }
}
