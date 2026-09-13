package uz.academixai.learning.application.port.out;

import uz.academixai.domain.SubjectType;

/** Optional deterministic correctness guard before asking the AI verifier. */
public interface UniqueTaskContentValidator {

  boolean isSolvable(SubjectType subjectType, String taskContent);
}
