package uz.academixai.identity.application;

import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.academixai.identity.application.port.out.AccountRepository;
import uz.academixai.identity.application.port.out.AttemptLimiter;
import uz.academixai.identity.application.port.out.PasswordResetNotifier;
import uz.academixai.identity.application.port.out.ResetTokenStore;
import uz.academixai.identity.domain.Account;
import uz.academixai.shared.error.ApiException;

/**
 * academix_tz.md §2.1 forgot-password/reset-password — ADMIN/TEACHER/PARENT/PSYCHOLOGIST only
 * (users with an {@code email}; STUDENT reset is a separate class-teacher-assisted flow, see {@code
 * uz.academixai.application.StudentPasswordResetService}). Redis one-time token, same shape as
 * {@code TelegramLinkService}'s deep-link token (this project's existing template for this exact
 * pattern) — single-use, deleted on consumption.
 *
 * <p><b>Anti-enumeration by design</b>: {@link #forgotPassword} always returns success and never
 * reveals whether the email address belongs to an active account. A caller cannot distinguish "no
 * such account" from "email genuinely sent" from the response alone; only real email delivery (or
 * its absence) tells them anything.
 *
 * <p>Rate limit: {@link AttemptLimiter} — the same keyed counter that guards login, because "count
 * attempts per key in a window, then refuse" is the same rule here.
 *
 * <p>Moved here from the legacy {@code application} package. It reached a JPA repository, a Redis
 * template and an SMTP sender directly; Identity already had an account port and a limiter, so the
 * move deleted two dependencies and added {@link ResetTokenStore} and {@link
 * PasswordResetNotifier}.
 */
@Service
public class PasswordResetService {

  private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);
  private static final int RATE_LIMIT_MAX = 3;

  private final AccountRepository accounts;
  private final AttemptLimiter attempts;
  private final ResetTokenStore tokens;
  private final PasswordResetNotifier notifier;
  private final PasswordEncoder passwordEncoder;

  @Value("${academix.frontend-url:http://localhost:3000}")
  private String frontendUrl;

  public PasswordResetService(
      AccountRepository accounts,
      AttemptLimiter attempts,
      ResetTokenStore tokens,
      PasswordResetNotifier notifier,
      PasswordEncoder passwordEncoder) {
    this.accounts = accounts;
    this.attempts = attempts;
    this.tokens = tokens;
    this.notifier = notifier;
    this.passwordEncoder = passwordEncoder;
  }

  public void forgotPassword(String email) {
    String normalizedEmail = email == null ? "" : email.trim();
    enforceRateLimit(normalizedEmail);
    accounts
        .findByEmail(normalizedEmail)
        .filter(Account::active)
        .ifPresent(
            account -> {
              String token = tokens.issue(account.id());
              notifier.sendResetLink(
                  account.email(), frontendUrl + "/reset-password?token=" + token);
            });
    // No branch returns anything different — success is unconditional, see class Javadoc.
  }

  public void resetPassword(String token, String newPassword) {
    PasswordPolicy.requireValid(newPassword);
    Account account =
        tokens
            .consume(token)
            .flatMap(accounts::findById)
            .orElseThrow(PasswordResetService::invalidToken);
    accounts.save(account.withPasswordHash(passwordEncoder.encode(newPassword)));
  }

  private void enforceRateLimit(String email) {
    long attemptsInWindow =
        attempts.record("password_reset_rate:" + email.toLowerCase(Locale.ROOT), RATE_LIMIT_WINDOW);
    if (attemptsInWindow > RATE_LIMIT_MAX) {
      throw new ApiException(
          HttpStatus.TOO_MANY_REQUESTS,
          "ERR_RATE_LIMIT",
          "Juda ko'p urinish. Keyinroq qayta urinib ko'ring.",
          "1 soatdan keyin qayta urinib ko'ring.");
    }
  }

  private static ApiException invalidToken() {
    return new ApiException(
        HttpStatus.BAD_REQUEST,
        "ERR_INVALID_RESET_TOKEN",
        "Havola yaroqsiz yoki muddati tugagan.",
        "Parolni tiklashni qaytadan so'rang.");
  }
}
