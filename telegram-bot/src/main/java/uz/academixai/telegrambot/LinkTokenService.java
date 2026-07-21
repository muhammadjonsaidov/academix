package uz.academixai.telegrambot;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Reads the SAME Redis instance backend's {@code TelegramLinkService} writes to — key format {@code
 * telegram_link_token:{token}} storing the userId that requested the link, 5-min TTL, single-use
 * (deleted on consumption). Backend generates the token (needs an authenticated JWT context, stays
 * there); this service only ever consumes it, once, when a user sends {@code /start <token>} in
 * Telegram — the two processes coordinate purely through shared Redis state, no direct call between
 * them.
 */
@Service
public class LinkTokenService {

  private final StringRedisTemplate redis;

  public LinkTokenService(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public Optional<UUID> consumeLinkToken(String token) {
    String key = tokenKey(token);
    String userId = redis.opsForValue().get(key);
    if (userId == null) {
      return Optional.empty();
    }
    redis.delete(key);
    return Optional.of(UUID.fromString(userId));
  }

  private static String tokenKey(String token) {
    return "telegram_link_token:" + token;
  }
}
