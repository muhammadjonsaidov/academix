package uz.academixai.intelligence.application.port.out;

import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.SubjectType;

/** Learning/School read boundary for deterministic assignment relevance checks. */
public interface AssignmentSubjectLookup {

  Optional<SubjectType> subjectType(UUID schoolId, UUID assignmentId);
}
