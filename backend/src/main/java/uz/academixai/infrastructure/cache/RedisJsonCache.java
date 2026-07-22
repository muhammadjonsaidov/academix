package uz.academixai.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Tiny read-through JSON cache over Redis for expensive, staleness-tolerant reads (admin
 * analytics aggregations). Deliberately manual (StringRedisTemplate + the legacy Jackson-2
 * {@code ObjectMapper} bean from {@code infrastructure/ai/JacksonConfig} — see CLAUDE.md's
 * Jackson-3-default finding) rather than Spring Cache abstraction: two small methods beat a new
 * cache-manager configuration surface, and every failure mode degrades to "just run the query."
 */
@Component
public class RedisJsonCache {

  private static final Logger log = LoggerFactory.getLogger(RedisJsonCache.class);

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;

  public RedisJsonCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
    this.redis = redis;
    this.objectMapper = objectMapper;
  }

  /** Read-through: cached JSON if present, else compute, store with TTL, return. */
  public <T> T getOrLoad(String key, Duration ttl, TypeReference<T> type, Supplier<T> loader) {
    try {
      String cached = redis.opsForValue().get(key);
      if (cached != null) {
        return objectMapper.readValue(cached, type);
      }
    } catch (Exception e) {
      log.warn("Cache read failed for {} — falling through to loader", key, e);
    }
    T value = loader.get();
    try {
      redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
    } catch (Exception e) {
      log.warn("Cache write failed for {}", key, e);
    }
    return value;
  }
}
