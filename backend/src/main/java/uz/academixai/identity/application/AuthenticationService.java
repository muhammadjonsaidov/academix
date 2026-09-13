package uz.academixai.identity.application;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.academixai.application.SchoolContextResolver;
import uz.academixai.identity.application.port.out.AccountRepository;
import uz.academixai.identity.domain.Account;
import uz.academixai.infrastructure.ratelimit.RedisRateLimiter;
import uz.academixai.infrastructure.security.JwtService;
import uz.academixai.infrastructure.security.RefreshTokenStore;
import uz.academixai.interfaces.web.ApiException;

/** Authentication and self-service account use cases. */
@Service
public class AuthenticationService {

  private static final int MAX_LOGIN_ATTEMPTS = 5;
  private static final Duration LOGIN_ATTEMPT_WINDOW = Duration.ofMinutes(1);
  private static final Duration LOGIN_BLOCK_DURATION = Duration.ofMinutes(15);

  private final AccountRepository accounts;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RefreshTokenStore refreshTokenStore;
  private final SchoolContextResolver schoolContextResolver;
  private final RedisRateLimiter rateLimiter;

  public AuthenticationService(
      AccountRepository accounts,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      RefreshTokenStore refreshTokenStore,
      SchoolContextResolver schoolContextResolver,
      RedisRateLimiter rateLimiter) {
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.refreshTokenStore = refreshTokenStore;
    this.schoolContextResolver = schoolContextResolver;
    this.rateLimiter = rateLimiter;
  }

  public record LoginResult(String accessToken, String refreshToken, Account account) {}

  public LoginResult login(String phone, String rawPassword) {
    enforceNotBlocked(phone);
    Account account = accounts.findByPhone(phone).filter(Account::active).orElse(null);
    if (account == null || !passwordEncoder.matches(rawPassword, account.passwordHash())) {
      recordFailedAttempt(phone);
      throw invalidCredentials();
    }

    rateLimiter.reset(loginAttemptsKey(phone));
    account = accounts.save(account.withLastLoginAt(LocalDateTime.now()));
    UUID schoolId = schoolContextResolver.resolve(account.toUser()).orElse(null);
    String accessToken = jwtService.issueAccessToken(account.id(), account.role(), schoolId);
    var refresh = jwtService.issueRefreshToken(account.id());
    refreshTokenStore.store(
        refresh.jti(), account.id(), Duration.ofSeconds(jwtService.refreshTokenTtlSeconds()));
    return new LoginResult(accessToken, refresh.token(), account);
  }

  public String refresh(String refreshToken) {
    var claims = parseRefreshOrThrow(refreshToken);
    if (!refreshTokenStore.isValid(claims.jti())) {
      throw expiredToken();
    }
    Account account =
        accounts
            .findById(claims.userId())
            .filter(Account::active)
            .orElseThrow(AuthenticationService::expiredToken);
    UUID schoolId = schoolContextResolver.resolve(account.toUser()).orElse(null);
    return jwtService.issueAccessToken(account.id(), account.role(), schoolId);
  }

  public void logout(UUID userId) {
    refreshTokenStore.revokeAllForUser(userId);
  }

  public Account profile(UUID userId) {
    return accounts.findById(userId).orElseThrow(AuthenticationService::userNotFound);
  }

  public Account updateProfile(UUID userId, String firstName, String lastName, String email) {
    if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_VALIDATION",
          "Ism va familiya bo'sh bo'lishi mumkin emas.",
          "Maydonlarni to'ldirib qayta urinib ko'ring.");
    }
    Account account = profile(userId);
    return accounts.save(
        account.withProfile(
            firstName.trim(),
            lastName.trim(),
            email == null || email.isBlank() ? null : email.trim()));
  }

  public void changePassword(UUID userId, String oldPassword, String newPassword) {
    Account account = profile(userId);
    if (!passwordEncoder.matches(oldPassword, account.passwordHash())) {
      throw invalidCredentials();
    }
    accounts.save(account.withPasswordHash(passwordEncoder.encode(newPassword)));
    refreshTokenStore.revokeAllForUser(userId);
  }

  private JwtService.RefreshTokenClaims parseRefreshOrThrow(String refreshToken) {
    try {
      return jwtService.parseRefreshToken(refreshToken);
    } catch (Exception exception) {
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
    return new ApiException(
        HttpStatus.UNAUTHORIZED,
        "ERR_EXPIRED_TOKEN",
        "Seans muddati tugadi. Tizimga qayta kiring.",
        "Client refresh token orqali access token olishi kerak.");
  }

  private static ApiException userNotFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND,
        "ERR_USER_NOT_FOUND",
        "Foydalanuvchi topilmadi.",
        "Qaytadan tizimga kiring.");
  }
}
