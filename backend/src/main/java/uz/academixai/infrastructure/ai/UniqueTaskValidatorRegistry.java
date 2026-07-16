package uz.academixai.infrastructure.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uz.academixai.domain.SubjectType;

/**
 * academix_tz.md §1.9 step 1 — looks up a registered {@link UniqueTaskValidator} by {@link
 * SubjectType}. Empty when none is registered, meaning the pre-check step is skipped entirely for
 * that subject (spec: "Validator yo'q fanlarda bu qadam o'tkazib yuboriladi").
 */
@Component
public class UniqueTaskValidatorRegistry {

  private final Map<SubjectType, UniqueTaskValidator> validators;

  public UniqueTaskValidatorRegistry(List<UniqueTaskValidator> registered) {
    this.validators =
        registered.stream()
            .collect(Collectors.toMap(UniqueTaskValidator::subjectType, Function.identity()));
  }

  public Optional<UniqueTaskValidator> forSubject(SubjectType subjectType) {
    return Optional.ofNullable(validators.get(subjectType));
  }
}
