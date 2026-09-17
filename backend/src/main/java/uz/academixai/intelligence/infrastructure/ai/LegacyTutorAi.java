package uz.academixai.intelligence.infrastructure.ai;

import org.springframework.stereotype.Component;
import uz.academixai.application.port.out.ai.AiProvider;
import uz.academixai.application.port.out.ai.AiProviderUnavailableException;
import uz.academixai.intelligence.application.port.out.TutorAi;

/** Compatibility adapter from the shared provider client to Intelligence tutor chat. */
@Component
public class LegacyTutorAi implements TutorAi {

  private final AiProvider provider;

  public LegacyTutorAi(AiProvider provider) {
    this.provider = provider;
  }

  @Override
  public String respond(String subjectContext, String studentMessage) {
    try {
      return provider.tutorChat(subjectContext, studentMessage);
    } catch (AiProviderUnavailableException exception) {
      throw new uz.academixai.intelligence.application.AiProviderUnavailableException(
          "AI provider tutor chat unavailable", exception);
    }
  }
}
