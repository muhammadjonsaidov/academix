package uz.academixai.learning.application.port.out;

import java.util.UUID;
import uz.academixai.domain.SubjectType;

/** School catalog data required to generate and validate a unique task. */
public interface UniqueTaskContextLookup {

  record Context(String subjectAndGrade, SubjectType subjectType) {}

  Context find(UUID subjectId, UUID classId);
}
