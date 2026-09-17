package uz.academixai.notification.adapter.in.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.notification.application.NotificationService;

/**
 * Deviation — no notification inbox endpoints exist in academix_tz.md at all (flagged repeatedly
 * across earlier sprints, see CLAUDE.md/ROADMAP.md); common to every role, scoped by JWT userId
 * only, no RLS/schoolId involvement (notifications isn't RLS-enabled).
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public List<NotificationResponse> list(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(defaultValue = "30") int limit) {
    return notificationService.listForUser(principal.userId(), limit).stream()
        .map(NotificationResponse::from)
        .toList();
  }

  @PutMapping("/{id}/read")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> markRead(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID id) {
    notificationService.markRead(principal.userId(), id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/read-all")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal AcademixPrincipal principal) {
    notificationService.markAllRead(principal.userId());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID id) {
    notificationService.delete(principal.userId(), id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> deleteAll(@AuthenticationPrincipal AcademixPrincipal principal) {
    notificationService.deleteAll(principal.userId());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/preferences")
  @PreAuthorize("isAuthenticated()")
  public List<NotificationPreferenceResponse> preferences(
      @AuthenticationPrincipal AcademixPrincipal principal) {
    return notificationService.preferences(principal.userId()).stream()
        .map(NotificationPreferenceResponse::from)
        .toList();
  }

  @PutMapping("/preferences")
  @PreAuthorize("isAuthenticated()")
  public List<NotificationPreferenceResponse> updatePreference(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @Valid @RequestBody UpdateNotificationPreferenceRequest request) {
    notificationService.updatePreference(
        principal.userId(), request.type(), request.inAppEnabled(), request.telegramEnabled());
    return notificationService.preferences(principal.userId()).stream()
        .map(NotificationPreferenceResponse::from)
        .toList();
  }
}
