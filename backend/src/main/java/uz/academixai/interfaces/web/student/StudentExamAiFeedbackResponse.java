package uz.academixai.interfaces.web.student;

import java.util.List;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.StepAnalysis;
import uz.academixai.infrastructure.persistence.ExamAIFeedbackEntity;

/**
 * academix_tz.md gap-fill (see StudentExamService) — handwriting score is deliberately NOT
 * included, same rule already applied to {@link StudentAiFeedbackResponse}.
 */
public record StudentExamAiFeedbackResponse(
    String feedback, List<CriteriaScore> criteriaScores, List<StepAnalysis> stepAnalyses) {

  public static StudentExamAiFeedbackResponse from(ExamAIFeedbackEntity entity) {
    if (entity == null) {
      return null;
    }
    var domain = entity.toDomain();
    return new StudentExamAiFeedbackResponse(
        domain.feedback(), domain.criteriaScores(), domain.stepAnalyses());
  }
}
