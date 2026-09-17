package uz.academixai.infrastructure.ratelimit;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uz.academixai.shared.error.ApiException;

/**
 * Shared fixed-window rate limiter backed by a plain Redis {@code INCR}+{@code EXPIRE} counter.
 *
 * <p><b>Why not Bucket4j</b>: {@code bucket4j-core}/{@code bucket4j-redis} have been on the
 * classpath since early sprints but were never wired to anything (no {@code ProxyManager}, no bean,
 * no usage) — CLAUDE.md records the decision to defer the real integration until a broad enough
 * need justified it. The counter pattern below is what {@link
 * uz.academixai.notification.application.TelegramLinkService} and {@link
 * uz.academixai.identity.application.PasswordResetService} each already hand-rolled; this class
 * exists so the three limits added afterwards (login, file upload, AI chat — academix_tz.md §5.3)
 * share one implementation instead of becoming copies three, four and five. Those two earlier
 * services are deliberately left untouched: they work, and rewriting live auth-adjacent code for
 * tidiness alone isn't worth the risk. Migrate them if either needs changing for another reason.
 *
 * <p>Fixed-window, not sliding: a caller can technically issue {@code 2 * max} requests across a
 * window boundary. Accepted deliberately — these limits exist to stop brute-force and runaway-cost
 * abuse, not to meter precisely, and a sliding window costs a sorted-set per key for no real gain
 * here.
 *
 * <p>Keys are caller-supplied and must be namespaced by the caller (e.g. {@code
 * login_attempts:{phone}}), matching the existing {@code ai_budget_*} / {@code
 * password_reset_rate:*} conventions.
 */
@Component
public class RedisRateLimiter {

  private final StringRedisTemplate redis;

  public RedisRateLimiter(StringRedisTemplate redis) {
    this.redis = redis;
  }

  /**
   * Records one hit against {@code key} and throws {@code ERR_RATE_LIMIT} if that pushes the count
   * past {@code max} within {@code window}.
   */
  public void enforce(String key, int max, Duration window, String message, String mitigation) {
    if (record(key, window) > max) {
      throw rateLimitExceeded(message, mitigation);
    }
  }

  /**
   * Increments the fixed-window counter for {@code key}, returning the new count. The TTL is set
   * only on the first hit, so the window runs from the first request rather than sliding forward on
   * every subsequent one (which would let a steady stream of traffic hold a key alive forever).
   */
  public long record(String key, Duration window) {
    Long count = redis.opsForValue().increment(key);
    if (count == null) {
      // Redis unreachable/misconfigured. Fail OPEN rather than locking every user out of a
      // working app over a cache outage — every limit built on this class is abuse mitigation,
      // never the sole control protecting correctness or authorization.
      return 0L;
    }
    if (count == 1L) {
      redis.expire(key, window);
    }
    return count;
  }

  /** Clears a counter — call after a legitimate success so honest users never accumulate. */
  public void reset(String key) {
    redis.delete(key);
  }

  /** Sets a standalone block marker that outlives the counter window. */
  public void block(String key, Duration duration) {
    redis.opsForValue().set(key, "1", duration);
  }

  public boolean isBlocked(String key) {
    return Boolean.TRUE.equals(redis.hasKey(key));
  }

  public ApiException rateLimitExceeded(String message, String mitigation) {
    // ERR_RATE_LIMIT — academix_backend_tdd.md error-code table.
    return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "ERR_RATE_LIMIT", message, mitigation);
  }
}
