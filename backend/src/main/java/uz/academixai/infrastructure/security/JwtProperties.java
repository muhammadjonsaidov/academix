package uz.academixai.infrastructure.security;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "academix.jwt")
public record JwtProperties(String secret, long accessTokenTtlMinutes, long refreshTokenTtlDays) {

  private static final int MIN_SECRET_BYTES = 32; // HS256 needs >= 256 bits

  public JwtProperties {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "JWT_SECRET must be at least "
              + MIN_SECRET_BYTES
              + " bytes (UTF-8) for HS256 — got "
              + (secret == null ? 0 : secret.getBytes(StandardCharsets.UTF_8).length)
              + ". Fails fast here instead of on the first token operation.");
    }
  }
}
