package uz.academixai.learning.application.port.out;

import java.util.List;
import java.util.UUID;
import uz.academixai.domain.CriteriaItem;

/**
 * Outbound port for a teacher's grading criteria.
 *
 * <p>Keyed by (subjectId, teacherId) alone — academix_tz.md §1.19 has no school column, so the pair
 * is the whole identity and one row exists per pair.
 */
public interface GradingCriteriaStore {

  /** The teacher's criteria for a subject, empty when they have not configured any yet. */
  List<CriteriaItem> criteria(UUID teacherId, UUID subjectId);

  /** Replaces the teacher's criteria for a subject, creating the row on first save. */
  void replace(UUID teacherId, UUID subjectId, List<CriteriaItem> criteria);
}
