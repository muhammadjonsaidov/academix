package uz.academixai.application;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.application.port.out.ai.GradingCriterion;
import uz.academixai.domain.CriteriaItem;
import uz.academixai.domain.SubjectGradingCriteria;
import uz.academixai.infrastructure.persistence.SubjectGradingCriteriaEntity;
import uz.academixai.infrastructure.persistence.SubjectGradingCriteriaRepository;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §1.19/§2.3 "Baholash mezonlari". No school scoping in the spec entity — a
 * teacher's criteria set is keyed by (subjectId, teacherId) alone, one row per pair.
 */
@Service
public class GradingCriteriaService {

  private final SubjectGradingCriteriaRepository repository;

  public GradingCriteriaService(SubjectGradingCriteriaRepository repository) {
    this.repository = repository;
  }

  /** Returns an empty list when the teacher hasn't configured criteria for this subject yet. */
  public List<CriteriaItem> get(UUID teacherId, UUID subjectId) {
    return repository
        .findBySubjectIdAndTeacherId(subjectId, teacherId)
        .map(SubjectGradingCriteriaEntity::toDomain)
        .map(SubjectGradingCriteria::criteria)
        .orElse(List.of());
  }

  /** academix_backend_tdd.md §6.4 — used by AIAnalysisService to build the Qwen grading prompt. */
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

    SubjectGradingCriteriaEntity entity =
        repository
            .findBySubjectIdAndTeacherId(subjectId, teacherId)
            .orElseGet(
                () ->
                    new SubjectGradingCriteriaEntity(
                        UUID.randomUUID(), subjectId, teacherId, criteria));
    SubjectGradingCriteria updated =
        new SubjectGradingCriteria(entity.getId(), subjectId, teacherId, criteria);
    repository.save(SubjectGradingCriteriaEntity.fromDomain(updated));
    return criteria;
  }
}
