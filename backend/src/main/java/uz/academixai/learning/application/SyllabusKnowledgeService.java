package uz.academixai.learning.application;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.learning.application.port.out.SyllabusKnowledgeLookup;
import uz.academixai.learning.application.port.out.SyllabusKnowledgeStore;
import uz.academixai.learning.application.port.out.TextEmbeddings;

/**
 * Retrieval API over the syllabus knowledge base — the grounding source for lesson plans and
 * unique-task generation. It returns only chunks the requesting teacher owns.
 *
 * <p>Implements Learning's own {@link SyllabusKnowledgeLookup} port: the workflow that needs
 * grounding calls the port, and this service is the implementation, so no anti-corruption adapter
 * sits between two classes of the same context.
 */
@Service
public class SyllabusKnowledgeService implements SyllabusKnowledgeLookup {

  private static final int MAX_RESULTS = 8;
  private static final String DEFAULT_QUERY = "darslik";

  private final SyllabusKnowledgeStore knowledge;
  private final TextEmbeddings embeddings;

  public SyllabusKnowledgeService(SyllabusKnowledgeStore knowledge, TextEmbeddings embeddings) {
    this.knowledge = knowledge;
    this.embeddings = embeddings;
  }

  public List<String> relevantForSyllabus(
      UUID teacherId, UUID syllabusId, String query, int limit) {
    return knowledge.searchBySyllabus(teacherId, syllabusId, embeddingOf(query), bounded(limit));
  }

  public List<String> relevantForClassAndSubject(
      UUID teacherId, UUID subjectId, UUID classId, String query, int limit) {
    return knowledge.searchByClassAndSubject(
        teacherId, subjectId, classId, embeddingOf(query), bounded(limit));
  }

  @Override
  public List<String> relevantForAssignment(
      UUID teacherId, UUID subjectId, UUID classId, String assignmentDescription) {
    return relevantForClassAndSubject(teacherId, subjectId, classId, assignmentDescription, 4);
  }

  private float[] embeddingOf(String query) {
    return embeddings.embedQuery(query == null || query.isBlank() ? DEFAULT_QUERY : query);
  }

  private static int bounded(int limit) {
    return Math.min(Math.max(limit, 1), MAX_RESULTS);
  }
}
