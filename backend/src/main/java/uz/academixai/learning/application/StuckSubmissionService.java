package uz.academixai.learning.application;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import uz.academixai.learning.application.port.out.StuckSubmissionRelease;

/**
 * Homework the AI never finished must not wait forever. A queue message is retried three times and
 * expires after ten minutes, so work still unchecked after {@link #GIVE_UP_AFTER} will not be
 * checked: it moves to {@code AI_SKIPPED}, the teacher's "grade by hand" tab. Nothing is lost — the
 * work stays as handed in. A late queue message finds the work no longer {@code SUBMITTED}, fails
 * its claim and leaves it alone. {@code AI_PROCESSING} is included because a backend restart
 * mid-analysis leaves work there, and its redelivered message can never claim it again.
 */
@Service
public class StuckSubmissionService {

  static final Duration GIVE_UP_AFTER = Duration.ofMinutes(30);

  private final StuckSubmissionRelease release;
  private final Clock clock;

  public StuckSubmissionService(StuckSubmissionRelease release, Clock clock) {
    this.release = release;
    this.clock = clock;
  }

  /** Returns how many submissions were handed back to their teachers. */
  public int releaseStuck() {
    return release.releaseSubmittedBefore(LocalDateTime.now(clock).minus(GIVE_UP_AFTER));
  }
}
