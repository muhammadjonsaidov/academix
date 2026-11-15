package uz.academixai.family.application.port.out;

import java.util.UUID;

/**
 * Erases the sensitive content of a minor's data that other contexts own: psychological evidence
 * (Wellbeing) and the feature vectors left behind by the retired handwriting-biometrics feature —
 * that table still holds historical rows until it is dropped, so a deletion request must still
 * clear them.
 *
 * <p>backend_tdd.md §7.6's pseudocode nulls the content and keeps the rows — aggregation and audit
 * survive, the biometric/psychological payload does not. That SQL talks to two tables Family does
 * not own, so it lives behind this port as an explicitly named compatibility adapter; the durable
 * shape is a request published to each owning context.
 */
public interface MinorDataEraser {

  int eraseHandwritingProfile(UUID studentId);

  int erasePsychologicalEvidence(UUID studentId);

  /** Retention job: resolved signals older than the retention window lose their evidence. */
  int anonymizeResolvedSignalsOlderThanTwoYears();
}
