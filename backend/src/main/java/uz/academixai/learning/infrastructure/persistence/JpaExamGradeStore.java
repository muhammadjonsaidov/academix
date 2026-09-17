package uz.academixai.learning.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.ExamGrade;
import uz.academixai.infrastructure.persistence.ExamGradeEntity;
import uz.academixai.infrastructure.persistence.ExamGradeRepository;
import uz.academixai.learning.application.port.out.ExamGradeStore;

/** JPA adapter for final exam-paper grades. */
@Repository
public class JpaExamGradeStore implements ExamGradeStore {

  private final ExamGradeRepository repository;

  public JpaExamGradeStore(ExamGradeRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<ExamGrade> findBySubmissionId(UUID submissionId) {
    return repository.findByExamSubmissionId(submissionId).map(ExamGradeEntity::toDomain);
  }

  @Override
  public ExamGrade save(ExamGrade grade) {
    return repository.save(ExamGradeEntity.fromDomain(grade)).toDomain();
  }
}
