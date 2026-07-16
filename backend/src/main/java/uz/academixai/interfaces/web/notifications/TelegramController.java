package uz.academixai.interfaces.web.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import uz.academixai.application.TelegramLinkService;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.infrastructure.telegram.TelegramProperties;

/**
 * academix_tz.md §2.7 "barcha rollar uchun umumiy" — Telegram sub-resource, common to every role.
 * {@code /webhook} is deliberately excluded from {@code @PreAuthorize}/JWT entirely (see {@code
 * SecurityConfig}'s {@code permitAll()} list) — Telegram calls it directly with no JWT, secured
 * only by the {@code X-Telegram-Bot-Api-Secret-Token} header (§7.5).
 *
 * <p>{@code @RequestBody} here binds to {@code tools.jackson.databind.JsonNode} (Jackson 3's own
 * package), not the legacy {@code com.fasterxml.jackson.databind.JsonNode} used elsewhere in this
 * codebase for outbound calls — confirmed by a real 500 ({@code InvalidDefinitionException: Cannot
 * construct instance of JsonNode, no Creators}) the first time this endpoint was actually hit. Boot
 * 4.1's primary {@code @RequestBody}/{@code @ResponseBody} message converter is Jackson-3-only and
 * can't instantiate the legacy abstract type without a concrete subtype hint. The legacy type still
 * works for outbound clients ({@code GoogleVisionClient}) because {@code RestClient} falls back
 * differently — this asymmetry is specific to inbound controller binding.
 */
@RestController
@RequestMapping("/api/v1/notifications/telegram")
public class TelegramController {

  private static final Logger log = LoggerFactory.getLogger(TelegramController.class);

  private final TelegramLinkService telegramLinkService;
  private final TelegramProperties properties;

  public TelegramController(
      TelegramLinkService telegramLinkService, TelegramProperties properties) {
    this.telegramLinkService = telegramLinkService;
    this.properties = properties;
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

  @PostMapping("/webhook")
  public ResponseEntity<Void> webhook(
      @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false)
          String secretHeader,
      @RequestBody JsonNode update) {
    if (properties.webhookSecret() == null || !properties.webhookSecret().equals(secretHeader)) {
      // §7.5: wrong/missing secret -> reject outright, no real Telegram server would ever fail
      // this check. Plain 401, not ApiException — Telegram doesn't parse our error JSON shape.
      return ResponseEntity.status(401).build();
    }

    JsonNode message = update.path("message");
    String text = message.path("text").asText("");
    if (!text.startsWith("/start ")) {
      return ResponseEntity.ok().build();
    }
    String token = text.substring("/start ".length()).trim();

    telegramLinkService
        .consumeLinkToken(token)
        .ifPresentOrElse(
            userId -> {
              long chatId = message.path("chat").path("id").asLong();
              String username = message.path("chat").path("username").asText(null);
              telegramLinkService.upsertConnection(userId, chatId, username);
            },
            () -> log.warn("Telegram webhook received an unknown/expired link token"));

    return ResponseEntity.ok().build();
  }
}
