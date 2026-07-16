package uz.academixai.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class HandwritingFeatureExtractorTest {

  private final HandwritingFeatureExtractor extractor = new HandwritingFeatureExtractor();

  @Test
  void returnsZeroVectorForEmptyLayout() {
    float[] vector = extractor.extract(new DocumentTextLayout(List.of()));

    assertThat(vector).hasSize(128);
    for (float v : vector) {
      assertThat(v).isZero();
    }
  }

  @Test
  void returns128Dimensions() {
    float[] vector = extractor.extract(sampleLayout(10, 20, 0.0));

    assertThat(vector).hasSize(128);
  }

  @Test
  void identicalLayoutsProduceIdenticalVectors() {
    float[] a = extractor.extract(sampleLayout(10, 20, 0.1));
    float[] b = extractor.extract(sampleLayout(10, 20, 0.1));

    assertThat(a).isEqualTo(b);
  }

  @Test
  void similarLayoutsAreMoreSimilarThanDissimilarOnes() {
    float[] reference = extractor.extract(sampleLayout(10, 20, 0.05));
    float[] closeMatch = extractor.extract(sampleLayout(11, 21, 0.06));
    float[] farMatch = extractor.extract(sampleLayout(30, 8, 0.8));

    double closeSimilarity = cosineSimilarity(reference, closeMatch);
    double farSimilarity = cosineSimilarity(reference, farMatch);

    assertThat(closeSimilarity).isGreaterThan(farSimilarity);
  }

  /** A synthetic 2-line, 2-word-per-line layout with a controllable char size and slant. */
  private static DocumentTextLayout sampleLayout(
      double charWidth, double charHeight, double slant) {
    List<CharacterBox> chars = new java.util.ArrayList<>();
    double y = 10;
    for (int line = 0; line < 2; line++) {
      double x = 10;
      for (int word = 0; word < 2; word++) {
        for (int ch = 0; ch < 4; ch++) {
          double xOffset = slant * charHeight;
          List<double[]> vertices =
              List.of(
                  new double[] {x + xOffset, y},
                  new double[] {x + charWidth + xOffset, y},
                  new double[] {x + charWidth, y + charHeight},
                  new double[] {x, y + charHeight});
          boolean lastCharOfWord = ch == 3;
          boolean lastWordOfLine = word == 1;
          CharacterBox.BreakType breakType =
              lastCharOfWord
                  ? (lastWordOfLine
                      ? CharacterBox.BreakType.LINE_BREAK
                      : CharacterBox.BreakType.SPACE)
                  : CharacterBox.BreakType.NONE;
          chars.add(new CharacterBox(String.valueOf((char) ('a' + ch)), vertices, breakType));
          x += charWidth + 2;
        }
        x += 10;
      }
      y += charHeight + 15;
    }
    return new DocumentTextLayout(chars);
  }

  private static double cosineSimilarity(float[] a, float[] b) {
    double dot = 0;
    double normA = 0;
    double normB = 0;
    for (int i = 0; i < a.length; i++) {
      dot += a[i] * b[i];
      normA += a[i] * a[i];
      normB += b[i] * b[i];
    }
    if (normA == 0 || normB == 0) {
      return 0;
    }
    return dot / (Math.sqrt(normA) * Math.sqrt(normB));
  }
}
