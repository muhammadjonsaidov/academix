package uz.academixai.application;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.academixai.domain.User;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.infrastructure.ratelimit.RedisRateLimiter;
import uz.academixai.infrastructure.security.JwtService;
import uz.academixai.infrastructure.security.RefreshTokenStore;
import uz.academixai.interfaces.web.ApiException;

@Service
public class AuthService {

  // academix_tz.md §5.3: "Login urinish: 5 marta/daqiqa, keyin 15 daqiqa bloklash".
  private static final int MAX_LOGIN_ATTEMPTS = 5;
  private static final Duration LOGIN_ATTEMPT_WINDOW = Duration.ofMinutes(1);
  private static final Duration LOGIN_BLOCK_DURATION = Duration.ofMinutes(15);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RefreshTokenStore refreshTokenStore;
  private final SchoolContextResolver schoolContextResolver;
  private final RedisRateLimiter rateLimiter;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      RefreshTokenStore refreshTokenStore,
      SchoolContextResolver schoolContextResolver,
      RedisRateLimiter rateLimiter) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.refreshTokenStore = refreshTokenStore;
    this.schoolContextResolver = schoolContextResolver;
    this.rateLimiter = rateLimiter;
  }

  public record LoginResult(String accessToken, String refreshToken, User user) {}

  public LoginResult login(String phone, String rawPassword) {
    enforceNotBlocked(phone);

    // Both failure branches are folded into one so a wrong password and an unknown phone are
    // indistinguishable to the caller AND count identically toward the block: incrementing only
    // for real accounts would turn the limiter itself into an account-enumeration oracle
    // (unknown phone -> unlimited attempts, real phone -> blocked after 5).
    Optional<UserEntity> found = userRepository.findByPhone(phone).filter(UserEntity::isActive);
    if (found.isEmpty() || !passwordEncoder.matches(rawPassword, found.get().getPasswordHash())) {
      recordFailedAttempt(phone);
      throw invalidCredentials();
    }
    UserEntity entity = found.get();
    // Honest user got in — clear the counter so yesterday's typos can't accumulate into a block.
    rateLimiter.reset(loginAttemptsKey(phone));

    entity.setLastLoginAt(LocalDateTime.now());
    userRepository.save(entity);

    User user = entity.toDomain();
    UUID schoolId = schoolContextResolver.resolve(user).orElse(null);

    String accessToken = jwtService.issueAccessToken(user.id(), user.role(), schoolId);
    var refresh = jwtService.issueRefreshToken(user.id());
    refreshTokenStore.store(
        refresh.jti(), user.id(), Duration.ofSeconds(jwtService.refreshTokenTtlSeconds()));

    return new LoginResult(accessToken, refresh.token(), user);
  }

  public String refresh(String refreshToken) {
    var claims = parseRefreshOrThrow(refreshToken);

    if (!refreshTokenStore.isValid(claims.jti())) {
      throw expiredToken();
    }

    UserEntity entity =
        userRepository
            .findById(claims.userId())
            .filter(UserEntity::isActive)
            .orElseThrow(AuthService::expiredToken);

    User user = entity.toDomain();
    UUID schoolId = schoolContextResolver.resolve(user).orElse(null);
    return jwtService.issueAccessToken(user.id(), user.role(), schoolId);
  }

  public void logout(UUID userId) {
    refreshTokenStore.revokeAllForUser(userId);
  }

  /** Current user's own profile — deviation, no /auth/profile in academix_tz.md §2.1. */
  public User profile(UUID userId) {
    return userRepository
        .findById(userId)
        .map(UserEntity::toDomain)
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.NOT_FOUND,
                    "ERR_USER_NOT_FOUND",
                    "Foydalanuvchi topilmadi.",
                    "Qaytadan tizimga kiring."));
  }

  /**
   * Self-service profile edit (name/email only — phone is the login identifier and stays
   * immutable). Deviation, same flag as {@link #profile}. Preserves {@code schoolId} via the 11-arg
   * constructor — the 10-arg one silently nulls it (see the RLS-wipe fix in git history).
   */
  public User updateProfile(UUID userId, String firstName, String lastName, String email) {
    UserEntity entity =
        userRepository
            .findById(userId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_USER_NOT_FOUND",
                        "Foydalanuvchi topilmadi.",
                        "Qaytadan tizimga kiring."));
    if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_VALIDATION",
          "Ism va familiya bo'sh bo'lishi mumkin emas.",
          "Maydonlarni to'ldirib qayta urinib ko'ring.");
    }
    UserEntity updated =
        new UserEntity(
            entity.getId(),
            firstName.trim(),
            lastName.trim(),
            entity.getPhone(),
            email == null || email.isBlank() ? null : email.trim(),
            entity.getPasswordHash(),
            entity.getRole(),
            entity.isActive(),
            entity.getCreatedAt(),
            entity.getLastLoginAt(),
            entity.getSchoolId());
    return userRepository.save(updated).toDomain();
  }

  public void changePassword(UUID userId, String oldPassword, String newPassword) {
    UserEntity entity =
        userRepository
            .findById(userId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_USER_NOT_FOUND",
                        "Foydalanuvchi topilmadi.",
                        "Qaytadan tizimga kiring."));

    if (!passwordEncoder.matches(oldPassword, entity.getPasswordHash())) {
      throw invalidCredentials();
    }

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
    refreshTokenStore.revokeAllForUser(userId);
  }

  private JwtService.RefreshTokenClaims parseRefreshOrThrow(String refreshToken) {
    try {
      return jwtService.parseRefreshToken(refreshToken);
    } catch (Exception e) {
      throw expiredToken();
    }
  }

  private void enforceNotBlocked(String phone) {
    if (rateLimiter.isBlocked(loginBlockKey(phone))) {
      throw rateLimiter.rateLimitExceeded(
          "Juda ko'p muvaffaqiyatsiz urinish. Hisobingiz vaqtincha bloklandi.",
          "15 daqiqadan keyin qayta urinib ko'ring yoki parolni tiklang.");
    }
  }

  private void recordFailedAttempt(String phone) {
    String attemptsKey = loginAttemptsKey(phone);
    if (rateLimiter.record(attemptsKey, LOGIN_ATTEMPT_WINDOW) >= MAX_LOGIN_ATTEMPTS) {
      // The block is a separate key with its own longer TTL: the counter's 1-minute window would
      // otherwise expire the block along with it, capping the lockout at a minute instead of 15.
      rateLimiter.block(loginBlockKey(phone), LOGIN_BLOCK_DURATION);
      rateLimiter.reset(attemptsKey);
    }
  }

  private static String loginAttemptsKey(String phone) {
    return "login_attempts:" + phone;
  }

  private static String loginBlockKey(String phone) {
    return "login_block:" + phone;
  }

  private static ApiException invalidCredentials() {
    return new ApiException(
        HttpStatus.UNAUTHORIZED,
        "ERR_INVALID_CREDENTIALS",
        "Telefon raqam yoki parol noto'g'ri.",
        "Ma'lumotlarni tekshirib qayta urinib ko'ring.");
  }

  private static ApiException expiredToken() {
    // ERR_EXPIRED_TOKEN — academix_backend_tdd.md error-code table
    return new ApiException(
        HttpStatus.UNAUTHORIZED,
        "ERR_EXPIRED_TOKEN",
        "Seans muddati tugadi. Tizimga qayta kiring.",
        "Client refresh token orqali access token olishi kerak.");
  }
}
