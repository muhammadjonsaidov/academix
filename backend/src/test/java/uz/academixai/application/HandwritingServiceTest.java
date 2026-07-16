package uz.academixai.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Pure-math unit tests for {@link HandwritingService}'s package-private static helpers — the exact
 * blending/scoring arithmetic from backend_tdd.md §6.2's pseudocode. Full check/update/ reset flow
 * is verified against the real live stack in task 51/52 (needs a real submission).
 */
class HandwritingServiceTest {

  @Test
  void cosineSimilarityOfIdenticalVectorsIsOne() {
    float[] v = {1f, 2f, 3f, 4f};

    assertThat(HandwritingService.cosineSimilarity(v, v)).isCloseTo(1.0f, offset(0.0001f));
  }

  @Test
  void cosineSimilarityOfOrthogonalVectorsIsZero() {
    float[] a = {1f, 0f};
    float[] b = {0f, 1f};

    assertThat(HandwritingService.cosineSimilarity(a, b)).isCloseTo(0.0f, offset(0.0001f));
  }

  @Test
  void cosineSimilarityIsZeroForZeroVector() {
    float[] a = {0f, 0f};
    float[] b = {1f, 2f};

    assertThat(HandwritingService.cosineSimilarity(a, b)).isZero();
  }

  @Test
  void interpolateBlendsTowardNewVectorByAlpha() {
    float[] oldVector = {0f, 0f};
    float[] newVector = {10f, 20f};

    float[] result = HandwritingService.interpolate(oldVector, newVector, 0.1f);

    assertThat(result[0]).isCloseTo(1.0f, offset(0.0001f));
    assertThat(result[1]).isCloseTo(2.0f, offset(0.0001f));
  }

  @Test
  void averageIsRunningMeanOverPriorSamples() {
    float[] oldVector = {10f};
    float[] newVector = {20f};

    // old is already the mean of 4 prior samples; adding a 5th sample of 20 should pull the
    // mean from 10 to 12.
    float[] result = HandwritingService.average(oldVector, newVector, 4);

    assertThat(result[0]).isCloseTo(12.0f, offset(0.0001f));
  }

  @Test
  void incrementVersionParsesNumericSuffix() {
    assertThat(HandwritingService.incrementVersion("v1")).isEqualTo("v2");
    assertThat(HandwritingService.incrementVersion("v9")).isEqualTo("v10");
    assertThat(HandwritingService.incrementVersion("v0")).isEqualTo("v1");
  }

  private static org.assertj.core.data.Offset<Float> offset(float value) {
    return org.assertj.core.data.Offset.offset(value);
  }
}
