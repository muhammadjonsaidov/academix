package uz.academixai.learning.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.ExamAIFeedback;
import uz.academixai.infrastructure.persistence.ExamAIFeedbackEntity;
import uz.academixai.infrastructure.persistence.ExamAIFeedbackRepository;
import uz.academixai.learning.application.port.out.ExamSubmissionAssessmentLookup;

/** JPA read adapter for AI exam assessments. */
@Repository
public class JpaExamSubmissionAssessmentLookup implements ExamSubmissionAssessmentLookup {

  private final ExamAIFeedbackRepository repository;

  public JpaExamSubmissionAssessmentLookup(ExamAIFeedbackRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<ExamAIFeedback> feedback(UUID submissionId) {
    return repository.findByExamSubmissionId(submissionId).map(ExamAIFeedbackEntity::toDomain);
  }
}
