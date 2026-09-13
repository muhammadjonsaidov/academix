package uz.academixai.learning.infrastructure.ai;

import org.springframework.stereotype.Component;
import uz.academixai.application.port.out.ai.AiProvider;
import uz.academixai.learning.application.port.out.UniqueTaskAi;

/** Explicit adapter until the Intelligence context publishes its generation API. */
@Component
public class LegacyUniqueTaskAi implements UniqueTaskAi {

  private final AiProvider provider;

  public LegacyUniqueTaskAi(AiProvider provider) {
    this.provider = provider;
  }

  @Override
  public String generate(String subjectAndGrade, String standardDescription) {
    return provider.generateUniqueTask(subjectAndGrade, standardDescription);
  }

  @Override
  public boolean verifySolvable(String taskContent) {
    return provider.verifyTaskSolvable(taskContent);
  }
}
