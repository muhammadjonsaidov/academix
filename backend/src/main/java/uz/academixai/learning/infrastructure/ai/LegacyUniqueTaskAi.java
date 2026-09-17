package uz.academixai.learning.infrastructure.ai;

import org.springframework.stereotype.Component;
import uz.academixai.learning.application.port.out.UniqueTaskAi;
import uz.academixai.shared.ai.AiProvider;

/** Explicit adapter until the Intelligence context publishes its generation API. */
@Component
public class LegacyUniqueTaskAi implements UniqueTaskAi {

  private final AiProvider provider;

  public LegacyUniqueTaskAi(AiProvider provider) {
    this.provider = provider;
  }

  @Override
  public String generate(
      String subjectAndGrade, String standardDescription, String syllabusContext) {
    return provider.generateUniqueTask(
        subjectAndGrade,
        syllabusContext == null || syllabusContext.isBlank()
            ? standardDescription
            : standardDescription + "\n\nDarslikdan olingan kontekst:\n" + syllabusContext);
  }

  @Override
  public boolean verifySolvable(String taskContent) {
    return provider.verifyTaskSolvable(taskContent);
  }
}
