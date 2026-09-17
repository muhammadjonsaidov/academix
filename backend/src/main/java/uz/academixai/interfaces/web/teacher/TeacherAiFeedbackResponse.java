package uz.academixai.interfaces.web.teacher;

import java.util.List;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.PlagiarismType;
import uz.academixai.domain.StepAnalysis;

/** academix_tz.md §2.3 — teacher sees everything, including plagiarism/handwriting. */
public record TeacherAiFeedbackResponse(
    String extractedText,
    float aiScorePercent,
    List<CriteriaScore> criteriaScores,
    List<StepAnalysis> stepAnalyses,
    float plagiarismScore,
    PlagiarismType plagiarismType,
    float handwritingMatchScore,
    String feedback) {

  public static TeacherAiFeedbackResponse from(AIFeedback feedback) {
    if (feedback == null) {
      return null;
    }
    return new TeacherAiFeedbackResponse(
        feedback.extractedText(),
        feedback.aiScorePercent(),
        feedback.criteriaScores(),
        feedback.stepAnalyses(),
        feedback.plagiarismScore(),
        feedback.plagiarismType(),
        feedback.handwritingMatchScore(),
        feedback.feedback());
  }
}
