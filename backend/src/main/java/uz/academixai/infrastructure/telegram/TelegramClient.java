package uz.academixai.infrastructure.telegram;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Plain Telegram Bot API {@code sendMessage} call — academix_tz.md §4's {@code
 * sendTelegramNotification(userId, message)} has no HTTP contract given anywhere in any spec doc
 * (confirmed by research), so this is standard Telegram Bot API usage, not spec-specific. No bot
 * exists in this dev environment ({@code TELEGRAM_BOT_TOKEN} is empty), so every real call here
 * fails — caught and logged, same graceful-degradation principle as an absent QWEN_API_KEY
 * elsewhere in this codebase. No retry queue (also undocumented) — a failed send just leaves {@code
 * Notification.sentToTelegram} false.
 */
@Component
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramClient {

  private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);
  private static final String API_BASE = "https://api.telegram.org/bot";

  private final RestClient restClient;
  private final TelegramProperties properties;

  public TelegramClient(RestClient.Builder restClientBuilder, TelegramProperties properties) {
    this.restClient = restClientBuilder.build();
    this.properties = properties;
  }

  /** Returns {@code true} if the send call succeeded, {@code false} on any failure (logged). */
  public boolean sendMessage(long chatId, String text) {
    if (properties.botToken() == null || properties.botToken().isBlank()) {
      log.warn("Telegram send skipped — no TELEGRAM_BOT_TOKEN configured (dev environment).");
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
