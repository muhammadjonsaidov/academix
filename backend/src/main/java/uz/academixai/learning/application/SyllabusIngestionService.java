package uz.academixai.learning.application;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uz.academixai.learning.application.port.out.SyllabusObjectStorage;
import uz.academixai.learning.application.port.out.SyllabusTextExtraction;
import uz.academixai.learning.application.port.out.TextEmbeddings;

/**
 * Orchestrates file download, extraction, chunking and vector indexing outside the HTTP request.
 */
@Service
public class SyllabusIngestionService {

  private static final Logger log = LoggerFactory.getLogger(SyllabusIngestionService.class);
  private static final int EMBEDDING_BATCH_SIZE = 10;

  private final SyllabusIngestionStateService state;
  private final SyllabusObjectStorage storage;
  private final SyllabusTextExtraction extractor;
  private final SyllabusChunker chunker;
  private final TextEmbeddings embeddings;

  public SyllabusIngestionService(
      SyllabusIngestionStateService state,
      SyllabusObjectStorage storage,
      SyllabusTextExtraction extractor,
      SyllabusChunker chunker,
      TextEmbeddings embeddings) {
    this.state = state;
    this.storage = storage;
    this.extractor = extractor;
    this.chunker = chunker;
    this.embeddings = embeddings;
  }

  public void ingest(UUID syllabusId) {
    try {
      SyllabusIngestionStateService.Source source = state.start(syllabusId);
      String extractedText =
          extractor.extract(source.fileType(), storage.download(source.objectKey()));
      List<String> chunks = chunker.chunk(extractedText);
      if (chunks.isEmpty()) {
        throw new IllegalArgumentException(
            "Fayldan yetarli matn olinmadi. Skan PDF uchun OCR yoki matnli PDF yuklang.");
      }
      state.replaceKnowledge(syllabusId, extractedText, embed(chunks));
      log.info("Syllabus {} indexed into {} semantic chunks", syllabusId, chunks.size());
    } catch (RuntimeException exception) {
      log.warn("Syllabus {} ingestion failed", syllabusId, exception);
      state.markFailed(syllabusId, userSafeMessage(exception));
      throw exception;
    }
  }

  private List<SyllabusIngestionStateService.Chunk> embed(List<String> chunks) {
    List<SyllabusIngestionStateService.Chunk> indexed = new ArrayList<>();
    for (int start = 0; start < chunks.size(); start += EMBEDDING_BATCH_SIZE) {
      List<String> batch =
          chunks.subList(start, Math.min(chunks.size(), start + EMBEDDING_BATCH_SIZE));
      List<float[]> vectors = embeddings.embedDocuments(batch);
      for (int offset = 0; offset < batch.size(); offset++) {
        String text = batch.get(offset);
        indexed.add(
            new SyllabusIngestionStateService.Chunk(
                text, SyllabusChunker.approximateTokenCount(text), vectors.get(offset)));
      }
    }
    return indexed;
  }

  private static String userSafeMessage(RuntimeException exception) {
    if (exception instanceof IllegalArgumentException
        && exception.getMessage() != null
        && exception.getMessage().startsWith("Fayldan")) {
      return exception.getMessage();
    }
    return "Darslikni AI uchun tayyorlashda xato yuz berdi. Qayta urinib ko'ring.";
  }
}
