package uz.academixai.reporting.application.port.out;

import java.time.LocalDateTime;
import java.util.UUID;

/** Outbound port for the assignment and submission activity the admin dashboard reports. */
public interface LearningActivityCounts {

  long assignments(UUID schoolId);

  /**
   * Distinct students who have submitted anything since {@code since} — the "active" proxy the
   * dashboard uses, see the legacy query's own Javadoc for why submission activity stands in for a
   * full activity log.
   */
  int distinctSubmittersSince(UUID schoolId, LocalDateTime since);
}
