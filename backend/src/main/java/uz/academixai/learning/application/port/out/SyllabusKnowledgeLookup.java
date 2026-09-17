package uz.academixai.learning.application.port.out;

import java.util.List;
import java.util.UUID;

/** Published knowledge-query port; Learning never reads another context's tables directly. */
public interface SyllabusKnowledgeLookup {

  List<String> relevantForAssignment(
      UUID teacherId, UUID subjectId, UUID classId, String assignmentDescription);
}
