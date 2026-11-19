package uz.academixai.shared.ai;

import java.util.List;

/** Outbound port for text-generation providers that expose chat completions. */
public interface AiProvider {

  AiGradingResult gradeSubmission(
      GradingMode mode,
      String subjectAndGrade,
      List<GradingCriterion> criteria,
      String extractedText);

  String tutorChat(String subjectAndContext, String studentMessage);

  PsychologyAnalysisResult analyzePsychology(String activitySummary);
}
