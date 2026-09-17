package uz.academixai.wellbeing.infrastructure.ai;

import org.springframework.stereotype.Component;
import uz.academixai.shared.ai.AiProvider;
import uz.academixai.shared.ai.AiProviderUnavailableException;
import uz.academixai.shared.ai.PsychologyAnalysisResult;
import uz.academixai.wellbeing.application.BehaviorAnalysisUnavailableException;
import uz.academixai.wellbeing.application.port.out.BehaviorAnalyzer;
import uz.academixai.wellbeing.domain.BehaviorAnalysis;
import uz.academixai.wellbeing.domain.SignalCandidate;

/** Compatibility adapter from the shared AI client to the Wellbeing behavior-analysis port. */
@Component
public class LegacyBehaviorAnalyzer implements BehaviorAnalyzer {

  private final AiProvider provider;

  public LegacyBehaviorAnalyzer(AiProvider provider) {
    this.provider = provider;
  }

  @Override
  public BehaviorAnalysis analyze(String activitySummary) {
    try {
      PsychologyAnalysisResult result = provider.analyzePsychology(activitySummary);
      return new BehaviorAnalysis(
          result.signals().stream()
              .map(
                  candidate ->
                      new SignalCandidate(
                          candidate.type(), candidate.severity(), candidate.evidence()))
              .toList(),
          result.isManipulationSuspected());
    } catch (AiProviderUnavailableException exception) {
      throw new BehaviorAnalysisUnavailableException(
          "Behavior analysis provider unavailable", exception);
    }
  }
}
