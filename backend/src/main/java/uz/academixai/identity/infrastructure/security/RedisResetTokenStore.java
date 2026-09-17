package uz.academixai.identity.infrastructure.security;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import uz.academixai.identity.application.port.out.ResetTokenStore;

/** Redis-backed {@link ResetTokenStore}: one key per token, deleted on consumption. */
@Component
public class RedisResetTokenStore implements ResetTokenStore {

  private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
  private static final String PREFIX = "password_reset_token:";

  private final StringRedisTemplate redis;

  public RedisResetTokenStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public String issue(UUID accountId) {
    String token = UUID.randomUUID().toString();
    redis.opsForValue().set(PREFIX + token, accountId.toString(), TOKEN_TTL);
    return token;
  }

  @Override
  public Optional<UUID> consume(String token) {
    String key = PREFIX + token;
    String value = redis.opsForValue().get(key);
    if (value == null) {
      return Optional.empty();
    }
    // Deleted before the caller can fail on anything else, so a token is spent the moment it is
    // presented — the same rule the inline version applied.
    redis.delete(key);
    return Optional.of(UUID.fromString(value));
  }
}
