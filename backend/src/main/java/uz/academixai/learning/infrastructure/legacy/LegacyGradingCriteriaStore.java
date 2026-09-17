package uz.academixai.learning.infrastructure.legacy;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.domain.CriteriaItem;
import uz.academixai.domain.SubjectGradingCriteria;
import uz.academixai.infrastructure.persistence.SubjectGradingCriteriaEntity;
import uz.academixai.infrastructure.persistence.SubjectGradingCriteriaRepository;
import uz.academixai.learning.application.port.out.GradingCriteriaStore;

/** Adapter for {@link GradingCriteriaStore} over the legacy criteria repository. */
@Component
public class LegacyGradingCriteriaStore implements GradingCriteriaStore {

  private final SubjectGradingCriteriaRepository repository;

  public LegacyGradingCriteriaStore(SubjectGradingCriteriaRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<CriteriaItem> criteria(UUID teacherId, UUID subjectId) {
    return repository
        .findBySubjectIdAndTeacherId(subjectId, teacherId)
        .map(SubjectGradingCriteriaEntity::toDomain)
        .map(SubjectGradingCriteria::criteria)
        .orElse(List.of());
  }

  @Override
  public void replace(UUID teacherId, UUID subjectId, List<CriteriaItem> criteria) {
    // Reuses the existing row's id when there is one, so a criteria edit updates rather than
    // re-inserts — the same rule the use case applied before this adapter existed.
    UUID id =
        repository
            .findBySubjectIdAndTeacherId(subjectId, teacherId)
            .map(SubjectGradingCriteriaEntity::getId)
            .orElseGet(UUID::randomUUID);
    repository.save(
        SubjectGradingCriteriaEntity.fromDomain(
            new SubjectGradingCriteria(id, subjectId, teacherId, criteria)));
  }
}
