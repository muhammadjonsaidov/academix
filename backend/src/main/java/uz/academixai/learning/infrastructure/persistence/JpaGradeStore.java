package uz.academixai.learning.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.Grade;
import uz.academixai.infrastructure.persistence.GradeEntity;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.learning.application.port.out.GradeStore;

/** Transitional JPA adapter for teacher grades. */
@Repository
public class JpaGradeStore implements GradeStore {

  private final GradeRepository repository;

  public JpaGradeStore(GradeRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<Grade> findBySubmissionId(UUID submissionId) {
    return repository.findBySubmissionId(submissionId).map(GradeEntity::toDomain);
  }

  @Override
  public Grade save(Grade grade) {
    return repository.save(GradeEntity.fromDomain(grade)).toDomain();
  }
}
