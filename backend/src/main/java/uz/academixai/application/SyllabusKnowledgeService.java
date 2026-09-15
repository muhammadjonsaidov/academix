package uz.academixai.application;

import com.pgvector.PGvector;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.application.port.out.ai.EmbeddingProvider;

/** Retrieval API for AI features. It returns only chunks the requesting teacher owns. */
@Service
public class SyllabusKnowledgeService {

  private final EntityManager entityManager;
  private final EmbeddingProvider embeddings;

  public SyllabusKnowledgeService(EntityManager entityManager, EmbeddingProvider embeddings) {
    this.entityManager = entityManager;
    this.embeddings = embeddings;
  }

  public List<String> relevantForSyllabus(
      UUID teacherId, UUID syllabusId, String query, int limit) {
    return find(
        "SELECT content FROM syllabus_chunks WHERE teacher_id = :teacherId AND syllabus_id = :syllabusId "
            + "ORDER BY embedding <=> CAST(:vector AS vector) LIMIT :limit",
        teacherId,
        syllabusId,
        null,
        null,
        query,
        limit);
  }

  public List<String> relevantForClassAndSubject(
      UUID teacherId, UUID subjectId, UUID classId, String query, int limit) {
    return find(
        "SELECT content FROM syllabus_chunks WHERE teacher_id = :teacherId AND subject_id = :subjectId "
            + "AND class_id = :classId ORDER BY embedding <=> CAST(:vector AS vector) LIMIT :limit",
        teacherId,
        null,
        subjectId,
        classId,
        query,
        limit);
  }

  @SuppressWarnings("unchecked")
  private List<String> find(
      String sql,
      UUID teacherId,
      UUID syllabusId,
      UUID subjectId,
      UUID classId,
      String query,
      int limit) {
    float[] vector = embeddings.embedQuery(query == null || query.isBlank() ? "darslik" : query);
    var nativeQuery =
        entityManager
            .createNativeQuery(sql)
            .setParameter("teacherId", teacherId)
            .setParameter("vector", vectorText(vector))
            .setParameter("limit", Math.min(Math.max(limit, 1), 8));
    if (syllabusId != null) {
      nativeQuery.setParameter("syllabusId", syllabusId);
    }
    if (subjectId != null) {
      nativeQuery.setParameter("subjectId", subjectId);
      nativeQuery.setParameter("classId", classId);
    }
    return nativeQuery.getResultList();
  }

  private static String vectorText(float[] vector) {
    return new PGvector(vector).getValue();
  }
}
