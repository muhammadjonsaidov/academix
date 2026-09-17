package uz.academixai.identity.infrastructure.ratelimit;

import java.time.Duration;
import org.springframework.stereotype.Component;
import uz.academixai.identity.application.port.out.LoginAttemptLimiter;
import uz.academixai.infrastructure.ratelimit.RedisRateLimiter;

/** Redis adapter for Identity's login-attempt rate-limit port. */
@Component
public class RedisLoginAttemptLimiter implements LoginAttemptLimiter {

  private final RedisRateLimiter rateLimiter;

  public RedisLoginAttemptLimiter(RedisRateLimiter rateLimiter) {
    this.rateLimiter = rateLimiter;
  }

  @Override
  public boolean isBlocked(String key) {
    return rateLimiter.isBlocked(key);
  }

  @Override
  public long record(String key, Duration window) {
    return rateLimiter.record(key, window);
  }

  @Override
  public void reset(String key) {
    rateLimiter.reset(key);
  }

  @Override
  public void block(String key, Duration duration) {
    rateLimiter.block(key, duration);
  }
}
