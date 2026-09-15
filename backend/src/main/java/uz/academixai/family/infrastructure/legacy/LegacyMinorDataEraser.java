package uz.academixai.family.infrastructure.legacy;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.family.application.port.out.MinorDataEraser;

/**
 * Compatibility adapter for backend_tdd.md §7.6's erasure pseudocode.
 *
 * <p>It reaches into two tables Family does not own — {@code handwriting_profiles} (Intelligence)
 * and {@code psychological_signals} (Wellbeing) — because no owning context publishes an erasure
 * API yet. Rows survive on purpose; only the biometric vector and the psychological evidence are
 * nulled, so audit and aggregation stay intact.
 */
@Component
public class LegacyMinorDataEraser implements MinorDataEraser {

  private static final String ERASE_HANDWRITING =
      "UPDATE handwriting_profiles SET feature_vector = NULL, is_reliable = FALSE"
          + " WHERE student_id = :studentId";

  private static final String ERASE_PSYCHOLOGICAL_EVIDENCE =
      "UPDATE psychological_signals SET raw_evidence = NULL, description = NULL"
          + " WHERE student_id = :studentId";

  private static final String RETENTION_SWEEP =
      "UPDATE psychological_signals SET description = NULL, raw_evidence = NULL"
          + " WHERE resolved = TRUE AND resolved_at < NOW() - INTERVAL '2 years'";

  private final EntityManager entityManager;

  public LegacyMinorDataEraser(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public int eraseHandwritingProfile(UUID studentId) {
    return entityManager
        .createNativeQuery(ERASE_HANDWRITING)
        .setParameter("studentId", studentId)
        .executeUpdate();
  }

  @Override
  public int erasePsychologicalEvidence(UUID studentId) {
    return entityManager
        .createNativeQuery(ERASE_PSYCHOLOGICAL_EVIDENCE)
        .setParameter("studentId", studentId)
        .executeUpdate();
  }

  @Override
  public int anonymizeResolvedSignalsOlderThanTwoYears() {
    return entityManager.createNativeQuery(RETENTION_SWEEP).executeUpdate();
  }
}
