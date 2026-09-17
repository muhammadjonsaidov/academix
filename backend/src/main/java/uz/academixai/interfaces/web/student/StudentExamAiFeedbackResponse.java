package uz.academixai.interfaces.web.student;

import java.util.List;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.ExamAIFeedback;
import uz.academixai.domain.StepAnalysis;

/**
 * academix_tz.md gap-fill — handwriting score is deliberately NOT included, same rule already
 * applied to {@link StudentAiFeedbackResponse}.
 */
public record StudentExamAiFeedbackResponse(
    String feedback, List<CriteriaScore> criteriaScores, List<StepAnalysis> stepAnalyses) {

  public static StudentExamAiFeedbackResponse from(ExamAIFeedback feedback) {
    if (feedback == null) {
      return null;
    }
    return new StudentExamAiFeedbackResponse(
        feedback.feedback(), feedback.criteriaScores(), feedback.stepAnalyses());
  }
}
