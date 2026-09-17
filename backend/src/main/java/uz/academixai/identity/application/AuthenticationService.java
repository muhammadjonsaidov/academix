package uz.academixai.identity.application;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.academixai.identity.application.port.out.AccessTokenIssuer;
import uz.academixai.identity.application.port.out.AccountRepository;
import uz.academixai.identity.application.port.out.AttemptLimiter;
import uz.academixai.identity.application.port.out.RefreshSessionStore;
import uz.academixai.identity.application.port.out.SchoolContextLookup;
import uz.academixai.identity.domain.Account;
import uz.academixai.shared.error.ApiException;

/** Authentication and self-service account use cases. */
@Service
public class AuthenticationService {

  private static final int MAX_LOGIN_ATTEMPTS = 5;
  private static final Duration LOGIN_ATTEMPT_WINDOW = Duration.ofMinutes(1);
  private static final Duration LOGIN_BLOCK_DURATION = Duration.ofMinutes(15);

  private final AccountRepository accounts;
  private final PasswordEncoder passwordEncoder;
  private final AccessTokenIssuer tokenIssuer;
  private final RefreshSessionStore refreshSessionStore;
  private final SchoolContextLookup schoolContextLookup;
  private final AttemptLimiter attemptLimiter;

  public AuthenticationService(
      AccountRepository accounts,
      PasswordEncoder passwordEncoder,
      AccessTokenIssuer tokenIssuer,
      RefreshSessionStore refreshSessionStore,
      SchoolContextLookup schoolContextLookup,
      AttemptLimiter attemptLimiter) {
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
    this.tokenIssuer = tokenIssuer;
    this.refreshSessionStore = refreshSessionStore;
    this.schoolContextLookup = schoolContextLookup;
    this.attemptLimiter = attemptLimiter;
  }

  public record LoginResult(String accessToken, String refreshToken, Account account) {}

  /** Access token plus a replacement refresh token created during rotation. */
  public record RefreshResult(String accessToken, String refreshToken) {}

  public LoginResult login(String identifier, String rawPassword) {
    String normalizedIdentifier = normalizeIdentifier(identifier);
    enforceNotBlocked(normalizedIdentifier);
    Account account =
        findAccountByIdentifier(normalizedIdentifier).filter(Account::active).orElse(null);
    if (account == null || !passwordEncoder.matches(rawPassword, account.passwordHash())) {
      recordFailedAttempt(normalizedIdentifier);
      throw invalidCredentials();
    }

    attemptLimiter.reset(loginAttemptsKey(normalizedIdentifier));
    account = accounts.save(account.withLastLoginAt(LocalDateTime.now()));
    UUID schoolId = schoolContextLookup.resolve(account).orElse(null);
    String accessToken = tokenIssuer.issueAccessToken(account.id(), account.role(), schoolId);
    var refresh = tokenIssuer.issueRefreshToken(account.id());
    refreshSessionStore.store(
        refresh.jti(), account.id(), Duration.ofSeconds(tokenIssuer.refreshTokenTtlSeconds()));
    return new LoginResult(accessToken, refresh.token(), account);
  }

  public RefreshResult refresh(String refreshToken) {
    var claims = parseRefreshOrThrow(refreshToken);
    if (!refreshSessionStore.consume(claims.jti(), claims.userId())) {
      throw expiredToken();
    }
    Account account =
        accounts
            .findById(claims.userId())
            .filter(Account::active)
            .orElseThrow(AuthenticationService::expiredToken);
    UUID schoolId = schoolContextLookup.resolve(account).orElse(null);
    // The old JTI was atomically consumed above. A replay now fails; the new JTI is stored first
    // so a response never contains a refresh token that the server cannot validate.
    var replacement = tokenIssuer.issueRefreshToken(account.id());
    refreshSessionStore.store(
        replacement.jti(), account.id(), Duration.ofSeconds(tokenIssuer.refreshTokenTtlSeconds()));
    return new RefreshResult(
        tokenIssuer.issueAccessToken(account.id(), account.role(), schoolId), replacement.token());
  }

  public void logout(UUID userId) {
    refreshSessionStore.revokeAllForUser(userId);
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
    PasswordPolicy.requireValid(newPassword);
    accounts.save(account.withPasswordHash(passwordEncoder.encode(newPassword)));
    refreshSessionStore.revokeAllForUser(userId);
  }

  private AccessTokenIssuer.RefreshTokenClaims parseRefreshOrThrow(String refreshToken) {
    try {
      return tokenIssuer.parseRefreshToken(refreshToken);
    } catch (Exception exception) {
      throw expiredToken();
    }
  }

  private void enforceNotBlocked(String identifier) {
    if (attemptLimiter.isBlocked(loginBlockKey(identifier))) {
      throw rateLimitExceeded(
          "Juda ko'p muvaffaqiyatsiz urinish. Hisobingiz vaqtincha bloklandi.",
          "15 daqiqadan keyin qayta urinib ko'ring yoki parolni tiklang.");
    }
  }

  private void recordFailedAttempt(String identifier) {
    String attemptsKey = loginAttemptsKey(identifier);
    if (attemptLimiter.record(attemptsKey, LOGIN_ATTEMPT_WINDOW) >= MAX_LOGIN_ATTEMPTS) {
      attemptLimiter.block(loginBlockKey(identifier), LOGIN_BLOCK_DURATION);
      attemptLimiter.reset(attemptsKey);
    }
  }

  private Optional<Account> findAccountByIdentifier(String identifier) {
    return identifier.contains("@")
        ? accounts.findByEmail(identifier)
        : accounts.findByPhone(identifier);
  }

  private static String normalizeIdentifier(String identifier) {
    String normalized = identifier == null ? "" : identifier.trim();
    return normalized.contains("@") ? normalized.toLowerCase(Locale.ROOT) : normalized;
  }

  private static String loginAttemptsKey(String identifier) {
    return "login_attempts:" + identifier;
  }

  private static String loginBlockKey(String identifier) {
    return "login_block:" + identifier;
  }

  private static ApiException invalidCredentials() {
    return new ApiException(
        HttpStatus.UNAUTHORIZED,
        "ERR_INVALID_CREDENTIALS",
        "Telefon raqam, email yoki parol noto'g'ri.",
        "Ma'lumotlarni tekshirib qayta urinib ko'ring.");
  }

  private static ApiException rateLimitExceeded(String message, String mitigation) {
    return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "ERR_RATE_LIMIT", message, mitigation);
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
