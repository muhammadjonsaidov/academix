package uz.academixai.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

    return new OcrResult(parseFullText(response));
  }

  /** academix_tz.md §3.1 response shape: {@code responses[0].fullTextAnnotation.text}. */
  static String parseFullText(JsonNode response) {
    if (response == null) {
      return "";
    }
    JsonNode textNode = response.path("responses").path(0).path("fullTextAnnotation").path("text");
    return textNode.isMissingNode() ? "" : textNode.asText();
  }

  @SuppressWarnings("unused") // invoked reflectively by resilience4j on circuit-open/failure
  private OcrResult extractTextFallback(byte[] imageBytes, Throwable cause) {
    throw new OcrUnavailableException("Google Vision OCR unavailable", cause);
  }
}
