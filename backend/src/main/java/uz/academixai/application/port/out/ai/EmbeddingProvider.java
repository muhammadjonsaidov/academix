package uz.academixai.application.port.out.ai;

import java.util.List;

/** Provider-neutral text vectorization port used by the syllabus knowledge base. */
public interface EmbeddingProvider {

  List<float[]> embedDocuments(List<String> texts);

  float[] embedQuery(String text);
}
