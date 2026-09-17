package uz.academixai.intelligence.infrastructure.ai;

import java.util.List;
import org.springframework.stereotype.Component;
import uz.academixai.intelligence.application.port.out.GradingAi;
import uz.academixai.intelligence.domain.GradingAnalysis;
import uz.academixai.intelligence.domain.GradingCriterion;
import uz.academixai.shared.ai.AiProvider;
import uz.academixai.shared.ai.AiProviderUnavailableException;

/** Compatibility adapter from the shared OpenAI-compatible client to the Intelligence port. */
@Component
public class LegacyGradingAi implements GradingAi {

  private final AiProvider provider;

  public LegacyGradingAi(AiProvider provider) {
    this.provider = provider;
  }

  @Override
  public GradingAnalysis grade(
      String subjectAndGrade, List<GradingCriterion> criteria, String text) {
    try {
      var result =
          provider.gradeSubmission(
              subjectAndGrade,
              criteria.stream()
                  .map(
                      item ->
                          new uz.academixai.shared.ai.GradingCriterion(
                              item.name(), item.weightPercent()))
                  .toList(),
              text);
      return new GradingAnalysis(
          result.criteriaScores(),
          result.feedback(),
          result.stepAnalyses(),
          result.plagiarismScore(),
          result.plagiarismType());
    } catch (AiProviderUnavailableException exception) {
      throw new uz.academixai.intelligence.application.AiProviderUnavailableException(
          "AI provider grading unavailable", exception);
    }
  }
}
