package uz.academixai.interfaces.web.teacher;

import java.util.List;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.PlagiarismType;
import uz.academixai.domain.StepAnalysis;
import uz.academixai.infrastructure.persistence.AIFeedbackEntity;

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

  public static TeacherAiFeedbackResponse from(AIFeedbackEntity entity) {
    if (entity == null) {
      return null;
    }
    var domain = entity.toDomain();
    return new TeacherAiFeedbackResponse(
        domain.extractedText(),
        domain.aiScorePercent(),
        domain.criteriaScores(),
        domain.stepAnalyses(),
        domain.plagiarismScore(),
        domain.plagiarismType(),
        domain.handwritingMatchScore(),
        domain.feedback());
  }
}
