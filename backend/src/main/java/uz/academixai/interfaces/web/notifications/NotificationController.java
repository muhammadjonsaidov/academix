package uz.academixai.interfaces.web.notifications;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.NotificationService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

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
  public List<NotificationResponse> list(@AuthenticationPrincipal AcademixPrincipal principal) {
    return notificationService.listForUser(principal.userId()).stream()
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
}
