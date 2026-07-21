package uz.academixai.telegrambot;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * Plain Telegram Bot API client — {@code getUpdates} (long-polling) + {@code sendMessage}. No SDK
 * dependency, matching this project's existing pattern for vendor HTTP calls (backend's
 * QwenAIClient/GoogleVisionClient are also plain RestClient, not vendor SDKs) — the Bot API surface
 * used here is small enough that a dependency isn't worth it, and avoids needing to verify a third
 * party SDK's Maven coordinates.
 *
 * <p>Uses Jackson 3's own {@code tools.jackson.databind.JsonNode} directly (not the legacy
 * com.fasterxml package) — this is a fresh service with no legacy Jackson 2 baggage, unlike
 * backend/ which had to work around Boot 4.1's Jackson-3-primary message converter for pre-existing
 * code (see CLAUDE.md's documented incident on QwenAIClient/GoogleVisionClient).
 */
@Component
@EnableConfigurationProperties(TelegramBotProperties.class)
public class TelegramApiClient {

  private static final Logger log = LoggerFactory.getLogger(TelegramApiClient.class);
  private static final String API_BASE = "https://api.telegram.org/bot";
  // Telegram's own recommended long-poll timeout — the connection blocks server-side up to this
  // many seconds waiting for a new update before returning an empty result.
  private static final int POLL_TIMEOUT_SECONDS = 30;

  private final RestClient restClient;
  private final TelegramBotProperties properties;

  public TelegramApiClient(RestClient.Builder restClientBuilder, TelegramBotProperties properties) {
    this.restClient = restClientBuilder.build();
    this.properties = properties;
  }

  /** Returns the raw {@code result} array of updates; empty list on timeout/no-update/failure. */
  public List<JsonNode> getUpdates(long offset) {
    if (properties.botToken() == null || properties.botToken().isBlank()) {
      log.warn("Telegram polling skipped — no TELEGRAM_BOT_TOKEN configured.");
      return List.of();
    }
    try {
      JsonNode response =
          restClient
              .get()
              .uri(
                  API_BASE
                      + properties.botToken()
                      + "/getUpdates?offset="
                      + offset
                      + "&timeout="
                      + POLL_TIMEOUT_SECONDS)
              .retrieve()
              .body(JsonNode.class);
      List<JsonNode> updates = new java.util.ArrayList<>();
      if (response != null) {
        response.path("result").forEach(updates::add);
      }
      return updates;
    } catch (Exception e) {
      // Real transient failures (network blip, Telegram-side hiccup) shouldn't kill the polling
      // loop — logged, next scheduled tick just tries again with the same unconsumed offset.
      log.warn("Telegram getUpdates failed, will retry on next poll", e);
      return List.of();
    }
  }

  /**
   * Returns {@code true} if the send succeeded, {@code false} on any failure (logged, not thrown —
   * matches this project's graceful-degradation convention for vendor calls).
   */
  public boolean sendMessage(long chatId, String text) {
    if (properties.botToken() == null || properties.botToken().isBlank()) {
      log.warn("Telegram send skipped — no TELEGRAM_BOT_TOKEN configured.");
      return false;
    }
    try {
      restClient
          .post()
          .uri(API_BASE + properties.botToken() + "/sendMessage")
          .body(Map.of("chat_id", chatId, "text", text))
          .retrieve()
          .toBodilessEntity();
      return true;
    } catch (Exception e) {
      log.warn("Telegram sendMessage failed for chatId {}", chatId, e);
      return false;
    }
  }
}
