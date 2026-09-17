package uz.academixai.learning.infrastructure.ai;

import java.util.List;
import org.springframework.stereotype.Component;
import uz.academixai.learning.application.port.out.TextEmbeddings;
import uz.academixai.shared.ai.EmbeddingProvider;

/**
 * Compatibility adapter: embedding is a provider capability (Qwen today) that lives in the legacy
 * AI client. Learning declares what it needs; this class is the only place that knows who provides
 * it — the shape Intelligence's published embedding API will replace.
 */
@Component
public class LegacyTextEmbeddings implements TextEmbeddings {

  private final EmbeddingProvider embeddings;

  public LegacyTextEmbeddings(EmbeddingProvider embeddings) {
    this.embeddings = embeddings;
  }

  @Override
  public List<float[]> embedDocuments(List<String> texts) {
    return embeddings.embedDocuments(texts);
  }

  @Override
  public float[] embedQuery(String text) {
    return embeddings.embedQuery(text);
  }
}
