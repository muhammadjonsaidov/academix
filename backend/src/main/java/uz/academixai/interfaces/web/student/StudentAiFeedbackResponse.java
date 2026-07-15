package uz.academixai.interfaces.web.student;

import java.util.List;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.StepAnalysis;
import uz.academixai.infrastructure.persistence.AIFeedbackEntity;

/**
 * academix_tz.md §2.4 — student view of AI feedback. Plagiarism/handwriting scores are deliberately
 * NOT included (spec's own explicit note: "plagiarism va handwriting score ko'RINMAYDI" — students
 * never see fraud-detection signals about themselves).
 */
public record StudentAiFeedbackResponse(
    String feedback, List<CriteriaScore> criteriaScores, List<StepAnalysis> stepAnalyses) {

  public static StudentAiFeedbackResponse from(AIFeedbackEntity entity) {
    if (entity == null) {
      return null;
    }
    var domain = entity.toDomain();
    return new StudentAiFeedbackResponse(
        domain.feedback(), domain.criteriaScores(), domain.stepAnalyses());
  }
}
