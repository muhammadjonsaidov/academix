package uz.academixai.learning.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import uz.academixai.learning.application.port.out.StuckSubmissionRelease;

class StuckSubmissionServiceTest {

  private static final ZoneId TASHKENT = ZoneId.of("Asia/Tashkent");

  @Test
  void releasesWorkHandedInMoreThanHalfAnHourAgo() {
    var clock = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), TASHKENT);
    var release = new RecordingRelease(2);

    int released = new StuckSubmissionService(release, clock).releaseStuck();

    assertThat(released).isEqualTo(2);
    assertThat(release.cutoff).isEqualTo(LocalDateTime.of(2026, 11, 20, 14, 30));
  }

  private static final class RecordingRelease implements StuckSubmissionRelease {
    private final int result;
    private LocalDateTime cutoff;

    RecordingRelease(int result) {
      this.result = result;
    }

    @Override
    public int releaseSubmittedBefore(LocalDateTime cutoff) {
      this.cutoff = cutoff;
      return result;
    }
  }
}
