package uz.academixai.learning.application;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.academixai.learning.application.port.out.SyllabusKnowledgeStore;
import uz.academixai.learning.application.port.out.SyllabusStore;

/** Small transactional boundary around ingestion state and chunk replacement. */
@Service
public class SyllabusIngestionStateService {

  private final SyllabusStore syllabuses;
  private final SyllabusKnowledgeStore knowledge;

  public SyllabusIngestionStateService(SyllabusStore syllabuses, SyllabusKnowledgeStore knowledge) {
    this.syllabuses = syllabuses;
    this.knowledge = knowledge;
  }

  @Transactional
  public Source start(UUID syllabusId) {
    SyllabusStore.Source source =
        syllabuses
            .startProcessing(syllabusId)
            .orElseThrow(() -> new IllegalArgumentException("Syllabus not found"));
    return new Source(
        source.id(),
        source.teacherId(),
        source.subjectId(),
        source.classId(),
        source.objectKey(),
        source.fileType());
  }

  @Transactional
  public void replaceKnowledge(UUID syllabusId, String extractedText, List<Chunk> chunks) {
    SyllabusStore.Source source =
        syllabuses
            .sourceOf(syllabusId)
            .orElseThrow(() -> new IllegalArgumentException("Syllabus not found"));
    knowledge.replaceChunks(
        new SyllabusKnowledgeStore.SyllabusScope(
            syllabusId, source.teacherId(), source.subjectId(), source.classId()),
        chunks.stream()
            .map(
                chunk ->
                    new SyllabusKnowledgeStore.Chunk(
                        chunk.content(), chunk.tokenCount(), chunk.embedding()))
            .toList());
    syllabuses.markReady(syllabusId, extractedText);
  }

  @Transactional
  public void markFailed(UUID syllabusId, String message) {
    syllabuses.markFailed(syllabusId, message);
  }

  public record Source(
      UUID id,
      UUID teacherId,
      UUID subjectId,
      UUID classId,
      String objectKey,
      uz.academixai.learning.domain.FileType fileType) {}

  public record Chunk(String content, int tokenCount, float[] embedding) {}
}
