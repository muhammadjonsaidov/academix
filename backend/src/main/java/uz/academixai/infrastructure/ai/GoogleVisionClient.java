package uz.academixai.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * academix_tz.md §3.1 — Google Cloud Vision {@code DOCUMENT_TEXT_DETECTION}, plain REST (API key
 * auth, not a service-account client — matches {@code GOOGLE_VISION_API_KEY} being a simple key, no
 * official Vision SDK dependency needed for this).
 *
 * <p><b>Deviation from the spec's literal request shape:</b> TZ §3.1's example sends {@code
 * image.source.imageUri} (a public URL). SeaweedFS is self-hosted and not internet-reachable, so
 * there's no URL Vision could fetch — this sends the image as inline base64 {@code image.content}
 * instead, which the same Vision API endpoint accepts. Functionally equivalent, just avoids
 * requiring SeaweedFS to be publicly exposed.
 */
@Component
@EnableConfigurationProperties(GoogleVisionProperties.class)
public class GoogleVisionClient {

  private static final String ANNOTATE_URL =
      "https://vision.googleapis.com/v1/images:annotate?key=";

  private final RestClient restClient;
  private final GoogleVisionProperties properties;

  public GoogleVisionClient(
      RestClient.Builder restClientBuilder, GoogleVisionProperties properties) {
    this.restClient = restClientBuilder.build();
    this.properties = properties;
  }

  @CircuitBreaker(name = "googleVision", fallbackMethod = "extractTextFallback")
  public OcrResult extractText(byte[] imageBytes) {
    Map<String, Object> requestBody =
        Map.of(
            "requests",
            List.of(
                Map.of(
                    "image", Map.of("content", Base64.getEncoder().encodeToString(imageBytes)),
                    "features", List.of(Map.of("type", "DOCUMENT_TEXT_DETECTION")))));

    JsonNode response =
        restClient
            .post()
            .uri(ANNOTATE_URL + properties.apiKey())
            .body(requestBody)
            .retrieve()
            .body(JsonNode.class);

    return new OcrResult(parseFullText(response), parseLayout(response));
  }

  /** academix_tz.md §3.1 response shape: {@code responses[0].fullTextAnnotation.text}. */
  static String parseFullText(JsonNode response) {
    if (response == null) {
      return "";
    }
    JsonNode textNode = response.path("responses").path(0).path("fullTextAnnotation").path("text");
    return textNode.isMissingNode() ? "" : textNode.asText();
  }

  /**
   * Drills into {@code fullTextAnnotation.pages[].blocks[].paragraphs[].words[].symbols[]} — the
   * spec's own example JSON stops at block level, but Vision's real {@code DOCUMENT_TEXT_DETECTION}
   * response goes all the way to per-character ({@code symbol}) bounding boxes, which is what
   * handwriting biometrics (TZ §1.13) needs. Each symbol's {@code property.detectedBreak.type} says
   * what follows it — {@code SPACE}/{@code SURE_SPACE} for a word gap, {@code
   * EOL_SURE_SPACE}/{@code LINE_BREAK} for a line break.
   */
  static DocumentTextLayout parseLayout(JsonNode response) {
    List<CharacterBox> characters = new ArrayList<>();
    if (response == null) {
      return new DocumentTextLayout(characters);
    }
    JsonNode pages = response.path("responses").path(0).path("fullTextAnnotation").path("pages");
    for (JsonNode page : pages) {
      for (JsonNode block : page.path("blocks")) {
        for (JsonNode paragraph : block.path("paragraphs")) {
          for (JsonNode word : paragraph.path("words")) {
            for (JsonNode symbol : word.path("symbols")) {
              characters.add(parseSymbol(symbol));
            }
          }
        }
      }
    }
    return new DocumentTextLayout(characters);
  }

  private static CharacterBox parseSymbol(JsonNode symbol) {
    List<double[]> vertices = new ArrayList<>();
    for (JsonNode vertex : symbol.path("boundingBox").path("vertices")) {
      vertices.add(new double[] {vertex.path("x").asDouble(0), vertex.path("y").asDouble(0)});
    }
    String breakType = symbol.path("property").path("detectedBreak").path("type").asText("");
    CharacterBox.BreakType breakAfter =
        switch (breakType) {
          case "SPACE", "SURE_SPACE" -> CharacterBox.BreakType.SPACE;
          case "EOL_SURE_SPACE", "LINE_BREAK" -> CharacterBox.BreakType.LINE_BREAK;
          default -> CharacterBox.BreakType.NONE;
        };
    return new CharacterBox(symbol.path("text").asText(""), vertices, breakAfter);
  }

  @SuppressWarnings("unused") // invoked reflectively by resilience4j on circuit-open/failure
  private OcrResult extractTextFallback(byte[] imageBytes, Throwable cause) {
    throw new OcrUnavailableException("Google Vision OCR unavailable", cause);
  }
}
