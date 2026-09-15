package uz.academixai.application;

import com.pgvector.PGvector;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.academixai.infrastructure.persistence.TeacherSyllabusEntity;
import uz.academixai.infrastructure.persistence.TeacherSyllabusRepository;

/** Small transactional boundary around ingestion state and chunk replacement. */
@Service
public class SyllabusIngestionStateService {

  private final TeacherSyllabusRepository syllabuses;
  private final EntityManager entityManager;

  public SyllabusIngestionStateService(
      TeacherSyllabusRepository syllabuses, EntityManager entityManager) {
    this.syllabuses = syllabuses;
    this.entityManager = entityManager;
  }

  @Transactional
  public Source start(UUID syllabusId) {
    TeacherSyllabusEntity syllabus =
        syllabuses
            .findById(syllabusId)
            .orElseThrow(() -> new IllegalArgumentException("Syllabus not found"));
    syllabus.markProcessing();
    return new Source(
        syllabus.getId(),
        syllabus.getTeacherId(),
        syllabus.getSubjectId(),
        syllabus.getClassId(),
        syllabus.getFileUrl(),
        syllabus.getFileType());
  }

  @Transactional
  public void replaceKnowledge(UUID syllabusId, String extractedText, List<Chunk> chunks) {
    TeacherSyllabusEntity syllabus =
        syllabuses
            .findById(syllabusId)
            .orElseThrow(() -> new IllegalArgumentException("Syllabus not found"));
    entityManager
        .createNativeQuery("DELETE FROM syllabus_chunks WHERE syllabus_id = :syllabusId")
        .setParameter("syllabusId", syllabusId)
        .executeUpdate();
    for (int index = 0; index < chunks.size(); index++) {
      Chunk chunk = chunks.get(index);
      entityManager
          .createNativeQuery(
              "INSERT INTO syllabus_chunks (id, syllabus_id, teacher_id, subject_id, class_id, chunk_index, content, token_count, embedding) "
                  + "VALUES (:id, :syllabusId, :teacherId, :subjectId, :classId, :chunkIndex, :content, :tokenCount, CAST(:embedding AS vector))")
          .setParameter("id", UUID.randomUUID())
          .setParameter("syllabusId", syllabusId)
          .setParameter("teacherId", syllabus.getTeacherId())
          .setParameter("subjectId", syllabus.getSubjectId())
          .setParameter("classId", syllabus.getClassId())
          .setParameter("chunkIndex", index)
          .setParameter("content", chunk.content())
          .setParameter("tokenCount", chunk.tokenCount())
          .setParameter("embedding", vectorText(chunk.embedding()))
          .executeUpdate();
    }
    syllabus.markReady(extractedText, LocalDateTime.now());
  }

  @Transactional
  public void markFailed(UUID syllabusId, String message) {
    syllabuses.findById(syllabusId).ifPresent(syllabus -> syllabus.markFailed(message));
  }

  private static String vectorText(float[] vector) {
    return new PGvector(vector).getValue();
  }

  public record Source(
      UUID id,
      UUID teacherId,
      UUID subjectId,
      UUID classId,
      String objectKey,
      uz.academixai.domain.FileType fileType) {}

  public record Chunk(String content, int tokenCount, float[] embedding) {}
}
