package uz.academixai.intelligence.infrastructure.redis;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import uz.academixai.intelligence.application.port.out.AiUsageCounter;
import uz.academixai.intelligence.domain.AiCallCategory;

/** Redis-backed AI usage counter that naturally expires after the tracked month. */
@Component
public class RedisAiUsageCounter implements AiUsageCounter {

  private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
  private static final Duration KEY_TTL = Duration.ofDays(40);

  private final StringRedisTemplate redis;

  public RedisAiUsageCounter(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public long currentUsage(UUID schoolId, AiCallCategory category) {
    String value = redis.opsForValue().get(key(schoolId, category));
    return value == null ? 0L : Long.parseLong(value);
  }

  @Override
  public void increment(UUID schoolId, AiCallCategory category) {
    Long newCount = redis.opsForValue().increment(key(schoolId, category));
    if (newCount != null && newCount == 1L) {
      redis.expire(key(schoolId, category), KEY_TTL);
    }
  }

  private static String key(UUID schoolId, AiCallCategory category) {
    return "ai_budget_"
        + category.name().toLowerCase()
        + ":"
        + schoolId
        + ":"
        + YEAR_MONTH.format(LocalDate.now());
  }
}
