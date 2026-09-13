package uz.academixai.interfaces.web.student;

import java.util.List;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.StepAnalysis;

/**
 * academix_tz.md §2.4 — student view of AI feedback. Plagiarism/handwriting scores are deliberately
 * NOT included (spec's own explicit note: "plagiarism va handwriting score ko'RINMAYDI" — students
 * never see fraud-detection signals about themselves).
 */
public record StudentAiFeedbackResponse(
    String feedback, List<CriteriaScore> criteriaScores, List<StepAnalysis> stepAnalyses) {

  public static StudentAiFeedbackResponse from(AIFeedback feedback) {
    if (feedback == null) {
      return null;
    }
    return new StudentAiFeedbackResponse(
        feedback.feedback(), feedback.criteriaScores(), feedback.stepAnalyses());
  }
}
