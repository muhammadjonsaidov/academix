package uz.academixai.learning.infrastructure.upload;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.ratelimit.UploadRateLimiter;
import uz.academixai.learning.application.port.out.UploadQuota;

/** Redis-backed upload quota adapter. */
@Component
public class RedisUploadQuota implements UploadQuota {

  private final UploadRateLimiter limiter;

  public RedisUploadQuota(UploadRateLimiter limiter) {
    this.limiter = limiter;
  }

  @Override
  public void enforce(UUID userId) {
    limiter.enforce(userId);
  }
}
