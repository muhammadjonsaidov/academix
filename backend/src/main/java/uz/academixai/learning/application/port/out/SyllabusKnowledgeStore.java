package uz.academixai.learning.application.port.out;

import java.util.List;
import java.util.UUID;

/**
 * The vector knowledge base behind lesson planning and unique-task generation: chunk persistence
 * (pgvector) plus the cosine-similarity searches that read it back.
 *
 * <p>The SQL stays in the adapter — the embedding column type and the ivfflat index are storage
 * concerns, and callers pass vectors rather than text so the query embedding is computed once by
 * the use case.
 */
public interface SyllabusKnowledgeStore {

  /** Replaces every chunk of a syllabus (delete + insert) with a freshly embedded set. */
  void replaceChunks(SyllabusScope scope, List<Chunk> chunks);

  List<String> searchBySyllabus(UUID teacherId, UUID syllabusId, float[] queryVector, int limit);

  List<String> searchByClassAndSubject(
      UUID teacherId, UUID subjectId, UUID classId, float[] queryVector, int limit);

  record SyllabusScope(UUID syllabusId, UUID teacherId, UUID subjectId, UUID classId) {}

  record Chunk(String content, int tokenCount, float[] embedding) {}
}
