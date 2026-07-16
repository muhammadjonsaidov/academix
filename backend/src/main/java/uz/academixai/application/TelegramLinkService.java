package uz.academixai.application;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.TelegramConnection;
import uz.academixai.infrastructure.persistence.TelegramConnectionEntity;
import uz.academixai.infrastructure.persistence.TelegramConnectionRepository;
import uz.academixai.infrastructure.telegram.TelegramProperties;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §2.7 "barcha rollar uchun umumiy" (common to all roles) Telegram sub-resource —
 * deep-link one-time-token flow. CLAUDE.md already documents the shape: "Link-token bir martalik,
 * Redis da 5 daqiqa TTL, ishlatilgach darhol o'chiriladi" (single-use, 5-min Redis TTL, deleted on
 * consumption) — followed exactly. No exact Redis key format is given anywhere, so {@code
 * telegram_link_token:{token}} is a judgment call, matching this project's other Redis key
 * conventions (e.g. {@code ai_budget_*}).
 *
 * <p><b>Rate limit deviation:</b> §2.7 says "Rate limit: 5 req/hour/user" but CLAUDE.md's decided
 * Bucket4j+Redis approach has zero real usage anywhere in this codebase yet (confirmed — only a
 * doc-comment mention, dependencies on the classpath but unwired). Formally wiring Bucket4j's Redis
 * {@code ProxyManager} (a real, non-trivial integration — codec, connection setup) for one
 * low-stakes endpoint is disproportionate; this uses a plain Redis {@code INCR}+{@code EXPIRE}
 * counter instead, the exact same real-time-counter pattern {@link AiBudgetService} already uses.
 * Adopting Bucket4j itself is deferred to whenever a broader rate-limiting need actually justifies
 * the integration cost — consistent with CLAUDE.md's "don't build for scale this project doesn't
 * have yet" principle.
 */
@Service
public class TelegramLinkService {

  private static final Duration TOKEN_TTL = Duration.ofMinutes(5);
  private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);
  private static final int RATE_LIMIT_MAX = 5;

  private final StringRedisTemplate redis;
  private final TelegramConnectionRepository connectionRepository;
  private final TelegramProperties properties;

  public TelegramLinkService(
      StringRedisTemplate redis,
      TelegramConnectionRepository connectionRepository,
      TelegramProperties properties) {
    this.redis = redis;
    this.connectionRepository = connectionRepository;
    this.properties = properties;
  }

  public record LinkTokenResult(String linkUrl, long expiresInSeconds) {}

  public LinkTokenResult generateLinkToken(UUID userId) {
    enforceRateLimit(userId);
    String token = UUID.randomUUID().toString();
    redis.opsForValue().set(tokenKey(token), userId.toString(), TOKEN_TTL);
    String linkUrl = "https://t.me/" + properties.botUsername() + "?start=" + token;
    return new LinkTokenResult(linkUrl, TOKEN_TTL.toSeconds());
  }

  /** Called by the webhook handler once, per Telegram's {@code /start <token>} message. */
  public Optional<UUID> consumeLinkToken(String token) {
    String key = tokenKey(token);
    String userId = redis.opsForValue().get(key);
    if (userId == null) {
      return Optional.empty();
    }
    redis.delete(key); // single-use — deleted immediately on consumption, per CLAUDE.md
    return Optional.of(UUID.fromString(userId));
  }

  public TelegramConnection upsertConnection(UUID userId, long chatId, String username) {
    TelegramConnectionEntity existing = connectionRepository.findByUserId(userId).orElse(null);
    TelegramConnection connection =
        new TelegramConnection(
            existing != null ? existing.toDomain().id() : UUID.randomUUID(),
            userId,
            chatId,
            username,
            true,
            existing != null ? existing.toDomain().connectedAt() : LocalDateTime.now());
    return connectionRepository.save(TelegramConnectionEntity.fromDomain(connection)).toDomain();
  }

  public void unlink(UUID userId) {
    connectionRepository.findByUserId(userId).ifPresent(connectionRepository::delete);
  }

  private void enforceRateLimit(UUID userId) {
    String key = "telegram_link_rate:" + userId;
    Long count = redis.opsForValue().increment(key);
    if (count != null && count == 1L) {
      redis.expire(key, RATE_LIMIT_WINDOW);
    }
    if (count != null && count > RATE_LIMIT_MAX) {
      throw new ApiException(
          HttpStatus.TOO_MANY_REQUESTS,
          "ERR_RATE_LIMIT",
          "Juda ko'p urinish. Keyinroq qayta urinib ko'ring.",
          "1 soatdan keyin qayta urinib ko'ring.");
    }
  }

  private static String tokenKey(String token) {
    return "telegram_link_token:" + token;
  }
}
