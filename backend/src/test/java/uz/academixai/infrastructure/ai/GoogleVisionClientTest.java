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

  @Test
  void parsesLayoutSymbolsWithBreaks() throws Exception {
    var response =
        objectMapper.readTree(
            """
            {
              "responses": [
                {
                  "fullTextAnnotation": {
                    "text": "Hi x",
                    "pages": [
                      {
                        "blocks": [
                          {
                            "paragraphs": [
                              {
                                "words": [
                                  {
                                    "symbols": [
                                      {
                                        "text": "H",
                                        "boundingBox": {
                                          "vertices": [
                                            {"x": 10, "y": 10}, {"x": 20, "y": 10},
                                            {"x": 20, "y": 30}, {"x": 10, "y": 30}
                                          ]
                                        }
                                      },
                                      {
                                        "text": "i",
                                        "boundingBox": {
                                          "vertices": [
                                            {"x": 21, "y": 10}, {"x": 26, "y": 10},
                                            {"x": 26, "y": 30}, {"x": 21, "y": 30}
                                          ]
                                        },
                                        "property": { "detectedBreak": { "type": "SPACE" } }
                                      }
                                    ]
                                  },
                                  {
                                    "symbols": [
                                      {
                                        "text": "x",
                                        "boundingBox": {
                                          "vertices": [
                                            {"x": 40, "y": 10}, {"x": 50, "y": 10},
                                            {"x": 50, "y": 30}, {"x": 40, "y": 30}
                                          ]
                                        },
                                        "property": { "detectedBreak": { "type": "LINE_BREAK" } }
                                      }
                                    ]
                                  }
                                ]
                              }
                            ]
                          }
                        ]
                      }
                    ]
                  }
                }
              ]
            }
            """);

    DocumentTextLayout layout = GoogleVisionClient.parseLayout(response);

    assertThat(layout.characters()).hasSize(3);
    assertThat(layout.characters().get(0).text()).isEqualTo("H");
    assertThat(layout.characters().get(0).breakAfter()).isEqualTo(CharacterBox.BreakType.NONE);
    assertThat(layout.characters().get(0).width()).isEqualTo(10.0);
    assertThat(layout.characters().get(0).height()).isEqualTo(20.0);
    assertThat(layout.characters().get(1).breakAfter()).isEqualTo(CharacterBox.BreakType.SPACE);
    assertThat(layout.characters().get(2).breakAfter())
        .isEqualTo(CharacterBox.BreakType.LINE_BREAK);
  }

  @Test
  void returnsEmptyLayoutForNullResponse() {
    assertThat(GoogleVisionClient.parseLayout(null).characters()).isEmpty();
  }
}
