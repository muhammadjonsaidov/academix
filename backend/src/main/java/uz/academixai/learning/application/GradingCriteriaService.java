package uz.academixai.learning.application;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.CriteriaItem;
import uz.academixai.learning.application.port.out.GradingCriteriaStore;
import uz.academixai.shared.ai.GradingCriterion;
import uz.academixai.shared.error.ApiException;

/**
 * academix_tz.md §1.19/§2.3 "Baholash mezonlari". No school scoping in the spec entity — a
 * teacher's criteria set is keyed by (subjectId, teacherId) alone, one row per pair.
 *
 * <p>Moved here from the legacy {@code application} package: criteria are a Learning concept, and
 * the persistence shape they happened to live in is now behind {@link GradingCriteriaStore}.
 */
@Service
public class GradingCriteriaService {

  private final GradingCriteriaStore store;

  public GradingCriteriaService(GradingCriteriaStore store) {
    this.store = store;
  }

  /** Returns an empty list when the teacher hasn't configured criteria for this subject yet. */
  public List<CriteriaItem> get(UUID teacherId, UUID subjectId) {
    return store.criteria(teacherId, subjectId);
  }

  /** academix_backend_tdd.md §6.4 — used by the AI grading pipeline to build the grading prompt. */
  public List<GradingCriterion> getForGrading(UUID teacherId, UUID subjectId) {
    return get(teacherId, subjectId).stream()
        .map(item -> new GradingCriterion(item.name(), item.weightPercent()))
        .toList();
  }

  public List<CriteriaItem> upsert(UUID teacherId, UUID subjectId, List<CriteriaItem> criteria) {
    if (criteria == null || criteria.isEmpty()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_INVALID_CRITERIA",
          "Kamida bitta mezon kerak.",
          "Kamida bitta baholash mezoni kiriting.");
    }
    int weightSum = criteria.stream().mapToInt(CriteriaItem::weightPercent).sum();
    if (weightSum != 100) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_INVALID_CRITERIA",
          "Mezonlar og'irligi jami 100% bo'lishi kerak.",
          "Og'irliklar yig'indisini 100% ga tenglashtiring.");
    }

    store.replace(teacherId, subjectId, criteria);
    return criteria;
  }
}
