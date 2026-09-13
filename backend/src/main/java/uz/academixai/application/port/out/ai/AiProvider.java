package uz.academixai.application.port.out.ai;

import java.util.List;
import uz.academixai.domain.LessonPlanContent;

/** Outbound port for text-generation providers that expose chat completions. */
public interface AiProvider {

  AiGradingResult gradeSubmission(
      String subjectAndGrade, List<GradingCriterion> criteria, String extractedText);

  LessonPlanContent generateLessonPlan(
      String subjectAndGrade, String topic, String syllabusExtractedContent);

  String generateUniqueTask(String subjectAndGrade, String standardTaskDescription);

  boolean verifyTaskSolvable(String taskContent);

  String tutorChat(String subjectAndContext, String studentMessage);

  PsychologyAnalysisResult analyzePsychology(String activitySummary);
}
