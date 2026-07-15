package uz.academixai.infrastructure.persistence;

import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Holds an uploaded bulk-import file between {@code analyze} (upload) and {@code commit} (consume)
 * — per bulk-import-wizard skill, {@code fileToken} is deliberately short-lived, not a permanent
 * reference, so this is Redis-backed with a TTL rather than SeaweedFS (which isn't wired into the
 * app yet, and would be the wrong tool for a scratch file anyway).
 */
@Component
public class TempImportFileStore {

  private static final String KEY_PREFIX = "import_file:";
  private static final Duration TTL = Duration.ofMinutes(10);

  private final StringRedisTemplate redis;

  public TempImportFileStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public String save(byte[] fileBytes) {
    String token = UUID.randomUUID().toString();
    redis.opsForValue().set(KEY_PREFIX + token, Base64.getEncoder().encodeToString(fileBytes), TTL);
    return token;
  }

  public Optional<byte[]> get(String token) {
    String encoded = redis.opsForValue().get(KEY_PREFIX + token);
    return Optional.ofNullable(encoded).map(Base64.getDecoder()::decode);
  }
}
