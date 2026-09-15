package uz.academixai.learning.infrastructure.persistence;

import com.pgvector.PGvector;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.learning.application.port.out.SyllabusKnowledgeStore;

/**
 * Adapter for the {@code syllabus_chunks} vector table. Kept on native SQL deliberately: the {@code
 * <=>} cosine operator and the {@code vector} cast are pgvector syntax, not JPA.
 */
@Repository
public class JdbcSyllabusKnowledgeStore implements SyllabusKnowledgeStore {

  private final EntityManager entityManager;

  public JdbcSyllabusKnowledgeStore(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public void replaceChunks(SyllabusScope scope, List<Chunk> chunks) {
    entityManager
        .createNativeQuery("DELETE FROM syllabus_chunks WHERE syllabus_id = :syllabusId")
        .setParameter("syllabusId", scope.syllabusId())
        .executeUpdate();
    for (int index = 0; index < chunks.size(); index++) {
      Chunk chunk = chunks.get(index);
      entityManager
          .createNativeQuery(
              "INSERT INTO syllabus_chunks (id, syllabus_id, teacher_id, subject_id, class_id,"
                  + " chunk_index, content, token_count, embedding)"
                  + " VALUES (:id, :syllabusId, :teacherId, :subjectId, :classId, :chunkIndex,"
                  + " :content, :tokenCount, CAST(:embedding AS vector))")
          .setParameter("id", UUID.randomUUID())
          .setParameter("syllabusId", scope.syllabusId())
          .setParameter("teacherId", scope.teacherId())
          .setParameter("subjectId", scope.subjectId())
          .setParameter("classId", scope.classId())
          .setParameter("chunkIndex", index)
          .setParameter("content", chunk.content())
          .setParameter("tokenCount", chunk.tokenCount())
          .setParameter("embedding", vectorText(chunk.embedding()))
          .executeUpdate();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> searchBySyllabus(
      UUID teacherId, UUID syllabusId, float[] queryVector, int limit) {
    return query(
        "SELECT content FROM syllabus_chunks WHERE teacher_id = :teacherId"
            + " AND syllabus_id = :syllabusId ORDER BY embedding <=> CAST(:vector AS vector)"
            + " LIMIT :limit",
        teacherId,
        syllabusId,
        null,
        null,
        queryVector,
        limit);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> searchByClassAndSubject(
      UUID teacherId, UUID subjectId, UUID classId, float[] queryVector, int limit) {
    return query(
        "SELECT content FROM syllabus_chunks WHERE teacher_id = :teacherId"
            + " AND subject_id = :subjectId AND class_id = :classId"
            + " ORDER BY embedding <=> CAST(:vector AS vector) LIMIT :limit",
        teacherId,
        null,
        subjectId,
        classId,
        queryVector,
        limit);
  }

  private List<String> query(
      String sql,
      UUID teacherId,
      UUID syllabusId,
      UUID subjectId,
      UUID classId,
      float[] queryVector,
      int limit) {
    var nativeQuery =
        entityManager
            .createNativeQuery(sql)
            .setParameter("teacherId", teacherId)
            .setParameter("vector", vectorText(queryVector))
            .setParameter("limit", Math.min(Math.max(limit, 1), 8));
    if (syllabusId != null) {
      nativeQuery.setParameter("syllabusId", syllabusId);
    }
    if (subjectId != null) {
      nativeQuery.setParameter("subjectId", subjectId);
    }
    if (classId != null) {
      nativeQuery.setParameter("classId", classId);
    }
    return nativeQuery.getResultList();
  }

  private static String vectorText(float[] vector) {
    return new PGvector(vector).getValue();
  }
}
