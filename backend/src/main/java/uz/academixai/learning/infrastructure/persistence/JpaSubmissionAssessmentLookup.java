package uz.academixai.learning.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.Grade;
import uz.academixai.infrastructure.persistence.AIFeedbackEntity;
import uz.academixai.infrastructure.persistence.AIFeedbackRepository;
import uz.academixai.infrastructure.persistence.GradeEntity;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.learning.application.port.out.SubmissionAssessmentLookup;

/** JPA adapter for feedback and grade values exposed to the owning student. */
@Repository
public class JpaSubmissionAssessmentLookup implements SubmissionAssessmentLookup {

  private final AIFeedbackRepository feedbacks;
  private final GradeRepository grades;

  public JpaSubmissionAssessmentLookup(AIFeedbackRepository feedbacks, GradeRepository grades) {
    this.feedbacks = feedbacks;
    this.grades = grades;
  }

  @Override
  public Optional<AIFeedback> feedback(UUID submissionId) {
    return feedbacks.findBySubmissionId(submissionId).map(AIFeedbackEntity::toDomain);
  }

  @Override
  public Optional<Grade> grade(UUID submissionId) {
    return grades.findBySubmissionId(submissionId).map(GradeEntity::toDomain);
  }
}
