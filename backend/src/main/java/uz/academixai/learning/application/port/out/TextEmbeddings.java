package uz.academixai.learning.application.port.out;

import java.util.List;

/**
 * Text vectorization for the knowledge base.
 *
 * <p>Learning-local port on purpose: the provider (Qwen today, anything tomorrow) is Intelligence's
 * concern, and the adapter that satisfies this port delegates to it. When Intelligence publishes
 * its embedding capability, this port is what the adapter will call.
 */
public interface TextEmbeddings {

  List<float[]> embedDocuments(List<String> texts);

  float[] embedQuery(String text);
}
