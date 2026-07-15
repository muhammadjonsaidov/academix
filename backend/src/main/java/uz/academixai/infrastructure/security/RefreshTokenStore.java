package uz.academixai.infrastructure.security;

import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Tracks which refresh-token jti's are still valid, so logout can revoke them before their 7-day
 * expiry. No rotation on refresh (TZ §2.1's /auth/refresh response only returns a new accessToken)
 * — a refresh token stays valid until logout or natural expiry.
 *
 * <p>TZ §2.1's {@code /auth/logout} only carries the access token (no refresh token in the
 * request), so there's no way to revoke one specific refresh token — this revokes all of the user's
 * active refresh tokens instead (log out everywhere), via a userId -&gt; jti-set reverse index.
 */
@Component
public class RefreshTokenStore {

  private static final String TOKEN_KEY_PREFIX = "refresh_token:";
  private static final String USER_INDEX_PREFIX = "refresh_tokens_by_user:";

  private final StringRedisTemplate redis;

  public RefreshTokenStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public void store(UUID jti, UUID userId, Duration ttl) {
    redis.opsForValue().set(TOKEN_KEY_PREFIX + jti, userId.toString(), ttl);
    redis.opsForSet().add(USER_INDEX_PREFIX + userId, jti.toString());
    redis.expire(USER_INDEX_PREFIX + userId, ttl);
  }

  public boolean isValid(UUID jti) {
    return Boolean.TRUE.equals(redis.hasKey(TOKEN_KEY_PREFIX + jti));
  }

  public void revokeAllForUser(UUID userId) {
    String indexKey = USER_INDEX_PREFIX + userId;
    var jtis = redis.opsForSet().members(indexKey);
    if (jtis != null) {
      jtis.forEach(jti -> redis.delete(TOKEN_KEY_PREFIX + jti));
    }
    redis.delete(indexKey);
  }
}
