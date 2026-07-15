package uz.academixai.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Pure parsing-logic test against the exact response shape documented in academix_tz.md §3.1 — no
 * network call (no real GOOGLE_VISION_API_KEY is available in this environment; see CLAUDE.md).
 */
class GoogleVisionClientTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void parsesFullTextFromDocumentedResponseShape() throws Exception {
    var response =
        objectMapper.readTree(
            """
            {
              "responses": [
                {
                  "fullTextAnnotation": {
                    "text": "Yechim: 5x^2 - 14x - 3 = 0..."
                  }
                }
              ]
            }
            """);

    assertThat(GoogleVisionClient.parseFullText(response))
        .isEqualTo("Yechim: 5x^2 - 14x - 3 = 0...");
  }

  @Test
  void returnsEmptyStringWhenNoTextDetected() throws Exception {
    var response =
        objectMapper.readTree(
            """
        { "responses": [ {} ] }
        """);

    assertThat(GoogleVisionClient.parseFullText(response)).isEmpty();
  }

  @Test
  void returnsEmptyStringForNullResponse() {
    assertThat(GoogleVisionClient.parseFullText(null)).isEmpty();
  }
}
