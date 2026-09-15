package uz.academixai.learning.infrastructure.knowledge;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.application.SyllabusKnowledgeService;
import uz.academixai.learning.application.port.out.SyllabusKnowledgeLookup;

/** Anti-corruption adapter from Learning's task workflow to the syllabus knowledge context. */
@Component
public class SyllabusKnowledgeLookupAdapter implements SyllabusKnowledgeLookup {

  private final SyllabusKnowledgeService knowledge;

  public SyllabusKnowledgeLookupAdapter(SyllabusKnowledgeService knowledge) {
    this.knowledge = knowledge;
  }

  @Override
  public List<String> relevantForAssignment(
      UUID teacherId, UUID subjectId, UUID classId, String assignmentDescription) {
    return knowledge.relevantForClassAndSubject(
        teacherId, subjectId, classId, assignmentDescription, 4);
  }
}
