package uz.academixai.interfaces.web.teacher;

import java.util.List;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.ExamAIFeedback;
import uz.academixai.domain.StepAnalysis;

public record ExamAiFeedbackResponse(
    String extractedText,
    List<StepAnalysis> stepAnalyses,
    List<CriteriaScore> criteriaScores,
    float aiScorePercent,
    String feedback,
    float handwritingMatchScore) {

  public static ExamAiFeedbackResponse from(ExamAIFeedback feedback) {
    if (feedback == null) {
      return null;
    }
    return new ExamAiFeedbackResponse(
        feedback.extractedText(),
        feedback.stepAnalyses(),
        feedback.criteriaScores(),
        feedback.aiScorePercent(),
        feedback.feedback(),
        feedback.handwritingMatchScore());
  }
}
