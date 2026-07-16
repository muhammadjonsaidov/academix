package uz.academixai.interfaces.web.notifications;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.Notification;
import uz.academixai.domain.NotificationType;

/** Deviation — no inbox response shape is documented in academix_tz.md, see NotificationService. */
public record NotificationResponse(
    UUID id,
    NotificationType type,
    String title,
    String body,
    String data,
    boolean isRead,
    boolean sentToTelegram,
    LocalDateTime createdAt,
    LocalDateTime readAt) {

  public static NotificationResponse from(Notification notification) {
    return new NotificationResponse(
        notification.id(),
        notification.type(),
        notification.title(),
        notification.body(),
        notification.data(),
        notification.isRead(),
        notification.sentToTelegram(),
        notification.createdAt(),
        notification.readAt());
  }
}
