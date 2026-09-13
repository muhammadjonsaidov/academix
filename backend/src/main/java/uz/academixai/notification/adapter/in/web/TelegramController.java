package uz.academixai.notification.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.notification.application.TelegramLinkService;

/**
 * academix_tz.md §2.7 "barcha rollar uchun umumiy" — Telegram sub-resource, common to every role.
 *
 * <p><b>No {@code /webhook} endpoint here anymore.</b> The Telegram bot moved to a standalone
 * service (see {@code telegram-bot/}) that long-polls the Bot API directly — no public HTTPS URL
 * needed (a real, documented constraint: Telegram's {@code setWebhook} requires one, which
 * localhost dev can never provide). This controller now only issues/manages link-tokens and
 * connection status — genuinely needs an authenticated JWT context, stays in the main backend. The
 * bot service consumes link-tokens from the same Redis instance and writes {@code
 * telegram_connections} directly; no call between the two services, coordinated purely through
 * shared Redis/Postgres state.
 */
@RestController
@RequestMapping("/api/v1/notifications/telegram")
public class TelegramController {

  private final TelegramLinkService telegramLinkService;

  public TelegramController(TelegramLinkService telegramLinkService) {
    this.telegramLinkService = telegramLinkService;
  }

  @PostMapping("/link-token")
  @PreAuthorize("isAuthenticated()")
  public LinkTokenResponse linkToken(@AuthenticationPrincipal AcademixPrincipal principal) {
    return LinkTokenResponse.from(telegramLinkService.generateLinkToken(principal.userId()));
  }

  @GetMapping("/status")
  @PreAuthorize("isAuthenticated()")
  public TelegramLinkService.ConnectionStatus status(
      @AuthenticationPrincipal AcademixPrincipal principal) {
    return telegramLinkService.status(principal.userId());
  }

  @DeleteMapping("/unlink")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> unlink(@AuthenticationPrincipal AcademixPrincipal principal) {
    telegramLinkService.unlink(principal.userId());
    return ResponseEntity.noContent().build();
  }
}
