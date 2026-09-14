package uz.academixai.application;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.academixai.identity.application.PasswordPolicy;
import uz.academixai.infrastructure.mail.PasswordResetMailSender;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §2.1 forgot-password/reset-password — ADMIN/TEACHER/PARENT/PSYCHOLOGIST only
 * (users with an {@code email}; STUDENT reset is a separate class-teacher-assisted flow, see {@link
 * StudentPasswordResetService}). Redis one-time token, same shape as {@link TelegramLinkService}'s
 * deep-link token (this project's existing template for this exact pattern) — {@code
 * password_reset_token:{token}} key, single-use, deleted on consumption.
 *
 * <p><b>Anti-enumeration by design</b>: {@link #forgotPassword} always returns success and never
 * reveals whether the email address belongs to an active account. A caller cannot distinguish
 * "no such account" from "email genuinely sent" from the response alone; only real email
 * delivery (or its absence) tells them anything.
 *
 * <p>Rate limit: plain Redis INCR+EXPIRE, same pattern (and same reasoning: Bucket4j is on the
 * classpath but unwired anywhere in this codebase) as {@link TelegramLinkService}.
 */
@Service
public class PasswordResetService {

  private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
  private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);
  private static final int RATE_LIMIT_MAX = 3;

  private final UserRepository userRepository;
  private final StringRedisTemplate redis;
  private final PasswordResetMailSender mailSender;
  private final PasswordEncoder passwordEncoder;

  @Value("${academix.frontend-url:http://localhost:3000}")
  private String frontendUrl;

  public PasswordResetService(
      UserRepository userRepository,
      StringRedisTemplate redis,
      PasswordResetMailSender mailSender,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.redis = redis;
    this.mailSender = mailSender;
    this.passwordEncoder = passwordEncoder;
  }

  public void forgotPassword(String email) {
    String normalizedEmail = email == null ? "" : email.trim();
    enforceRateLimit(normalizedEmail);
    Optional<UserEntity> user =
        userRepository.findFirstByEmailIgnoreCase(normalizedEmail).filter(UserEntity::isActive);
    if (user.isPresent()) {
      String token = UUID.randomUUID().toString();
      redis.opsForValue().set(tokenKey(token), user.get().getId().toString(), TOKEN_TTL);
      String resetUrl = frontendUrl + "/reset-password?token=" + token;
      mailSender.sendResetLink(user.get().getEmail(), resetUrl);
    }
    // No branch returns anything different — success is unconditional, see class Javadoc.
  }

  public void resetPassword(String token, String newPassword) {
    PasswordPolicy.requireValid(newPassword);
    String key = tokenKey(token);
    String userId = redis.opsForValue().get(key);
    if (userId == null) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_INVALID_RESET_TOKEN",
          "Havola yaroqsiz yoki muddati tugagan.",
          "Parolni tiklashni qaytadan so'rang.");
    }
    redis.delete(key); // single-use — deleted immediately on consumption

    UserEntity entity =
        userRepository
            .findById(UUID.fromString(userId))
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "ERR_INVALID_RESET_TOKEN",
                        "Havola yaroqsiz yoki muddati tugagan.",
                        "Parolni tiklashni qaytadan so'rang."));
    UserEntity updated =
        new UserEntity(
            entity.getId(),
            entity.getFirstName(),
            entity.getLastName(),
            entity.getPhone(),
            entity.getEmail(),
            passwordEncoder.encode(newPassword),
            entity.getRole(),
            entity.isActive(),
            entity.getCreatedAt(),
            entity.getLastLoginAt(),
            entity.getSchoolId());
    userRepository.save(updated);
  }

  private void enforceRateLimit(String email) {
    String key = "password_reset_rate:" + email.toLowerCase(java.util.Locale.ROOT);
    Long count = redis.opsForValue().increment(key);
    if (count != null && count == 1L) {
      redis.expire(key, RATE_LIMIT_WINDOW);
    }
    if (count != null && count > RATE_LIMIT_MAX) {
      throw new ApiException(
          HttpStatus.TOO_MANY_REQUESTS,
          "ERR_RATE_LIMIT",
          "Juda ko'p urinish. Keyinroq qayta urinib ko'ring.",
          "1 soatdan keyin qayta urinib ko'ring.");
    }
  }

  private static String tokenKey(String token) {
    return "password_reset_token:" + token;
  }
}
