package uz.academixai.intelligence.infrastructure.ratelimit;

import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.ratelimit.RedisRateLimiter;
import uz.academixai.intelligence.application.port.out.ChatAbuseGuard;

/** Redis-backed per-student chat rate-limit adapter. */
@Component
public class RedisChatAbuseGuard implements ChatAbuseGuard {

  private static final int MAX_CHATS_PER_MINUTE = 30;
  private static final Duration WINDOW = Duration.ofMinutes(1);

  private final RedisRateLimiter limiter;

  public RedisChatAbuseGuard(RedisRateLimiter limiter) {
    this.limiter = limiter;
  }

  @Override
  public void enforce(UUID studentId) {
    limiter.enforce(
        "ai_chat_rate:" + studentId,
        MAX_CHATS_PER_MINUTE,
        WINDOW,
        "Juda ko'p savol yubordingiz. Bir daqiqadan keyin qayta urinib ko'ring.",
        "Daqiqasiga " + MAX_CHATS_PER_MINUTE + " tagacha savol berish mumkin.");
  }
}
