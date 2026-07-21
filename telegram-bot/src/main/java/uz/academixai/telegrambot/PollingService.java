package uz.academixai.telegrambot;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Continuous long-polling loop — no manual trigger, no webhook, nothing for a human to click.
 * {@code getUpdates} blocks server-side up to 30s per call (see {@link TelegramApiClient}), so a
 * {@code fixedDelay=1000} schedule is effectively a tight continuous loop, not a slow poll: each
 * invocation either returns near-instantly with real updates, or blocks the full 30s with none,
 * then immediately re-polls.
 *
 * <p>The last-consumed {@code update_id} is persisted in Redis (shared with backend, key {@code
 * telegram_bot_update_offset}) rather than kept in memory — a restart must not re-process (and
 * re-send duplicate replies for) updates already handled before the restart.
 */
@Component
public class PollingService {

  private static final Logger log = LoggerFactory.getLogger(PollingService.class);
  private static final String OFFSET_KEY = "telegram_bot_update_offset";
  private static final String START_PREFIX = "/start ";

  private final TelegramApiClient apiClient;
  private final LinkTokenService linkTokenService;
  private final TelegramConnectionRepository connectionRepository;
  private final StringRedisTemplate redis;

  public PollingService(
      TelegramApiClient apiClient,
      LinkTokenService linkTokenService,
      TelegramConnectionRepository connectionRepository,
      StringRedisTemplate redis) {
    this.apiClient = apiClient;
    this.linkTokenService = linkTokenService;
    this.connectionRepository = connectionRepository;
    this.redis = redis;
  }

  @Scheduled(fixedDelay = 1000)
  public void poll() {
    long offset = currentOffset();
    List<JsonNode> updates = apiClient.getUpdates(offset);
    if (updates.isEmpty()) {
      return;
    }
    long maxUpdateId = offset - 1;
    for (JsonNode update : updates) {
      long updateId = update.path("update_id").asLong();
      maxUpdateId = Math.max(maxUpdateId, updateId);
      handleUpdate(update);
    }
    redis.opsForValue().set(OFFSET_KEY, String.valueOf(maxUpdateId + 1));
  }

  private void handleUpdate(JsonNode update) {
    JsonNode message = update.path("message");
    String text = message.path("text").asText("");
    if (!text.startsWith(START_PREFIX)) {
      return;
    }
    String token = text.substring(START_PREFIX.length()).trim();
    long chatId = message.path("chat").path("id").asLong();

    linkTokenService
        .consumeLinkToken(token)
        .ifPresentOrElse(
            userId -> {
              String username = message.path("chat").path("username").asText(null);
              connectionRepository.upsertConnection(userId, chatId, username);
              apiClient.sendMessage(
                  chatId, "Ulanish muvaffaqiyatli! Endi bildirishnomalarni shu yerda olasiz.");
              log.info("Telegram connection established for user {}", userId);
            },
            () -> {
              log.warn("Telegram /start received an unknown/expired link token");
              apiClient.sendMessage(
                  chatId, "Havola muddati tugagan yoki noto'g'ri. Ilovada qaytadan havola oling.");
            });
  }

  private long currentOffset() {
    String stored = redis.opsForValue().get(OFFSET_KEY);
    return stored == null ? 0L : Long.parseLong(stored);
  }
}
