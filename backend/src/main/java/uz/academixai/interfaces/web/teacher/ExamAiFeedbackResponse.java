package uz.academixai.interfaces.web.teacher;

import java.util.List;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.StepAnalysis;
import uz.academixai.infrastructure.persistence.ExamAIFeedbackEntity;

public record ExamAiFeedbackResponse(
    String extractedText,
    List<StepAnalysis> stepAnalyses,
    List<CriteriaScore> criteriaScores,
    float aiScorePercent,
    String feedback,
    float handwritingMatchScore) {

  public static ExamAiFeedbackResponse from(ExamAIFeedbackEntity entity) {
    if (entity == null) {
      return null;
    }
    var domain = entity.toDomain();
    return new ExamAiFeedbackResponse(
        domain.extractedText(),
        domain.stepAnalyses(),
        domain.criteriaScores(),
        domain.aiScorePercent(),
        domain.feedback(),
        domain.handwritingMatchScore());
  }
}
