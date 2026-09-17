package uz.academixai.progress.application.port.out;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbound port for a student's average grade over a time window — the only grade fact Progress'
 * growth figure needs.
 *
 * <p>Progress does not own grades (Learning/Reporting do), so it asks through a port instead of
 * reaching into the persistence that serves them.
 */
public interface GradeAverageLookup {

  /** Mean score of the grades this student received in {@code [since, until)}, or 0 when none. */
  double averageScore(UUID studentId, LocalDateTime since, LocalDateTime until);
}
