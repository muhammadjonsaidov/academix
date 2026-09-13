package uz.academixai.learning.infrastructure.ai;

import org.springframework.stereotype.Component;
import uz.academixai.domain.SubjectType;
import uz.academixai.infrastructure.ai.UniqueTaskValidatorRegistry;
import uz.academixai.learning.application.port.out.UniqueTaskContentValidator;

/** Adapter around the existing deterministic subject-specific validation registry. */
@Component
public class RegistryUniqueTaskContentValidator implements UniqueTaskContentValidator {

  private final UniqueTaskValidatorRegistry registry;

  public RegistryUniqueTaskContentValidator(UniqueTaskValidatorRegistry registry) {
    this.registry = registry;
  }

  @Override
  public boolean isSolvable(SubjectType subjectType, String taskContent) {
    return subjectType == null
        || registry.forSubject(subjectType).map(v -> v.isSolvable(taskContent)).orElse(true);
  }
}
