package uz.academixai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Notification;
import uz.academixai.domain.NotificationType;
import uz.academixai.infrastructure.persistence.NotificationEntity;
import uz.academixai.infrastructure.persistence.NotificationRepository;
import uz.academixai.infrastructure.queue.NotificationTelegramQueueProducer;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §1.15 / §4 {@code sendNotification(userId, type, params)}. Always persists a
 * {@link Notification} row (inbox is the source of truth); Telegram delivery is async on top —
 * enqueued via {@link NotificationTelegramQueueProducer} (only after this transaction commits, so
 * the standalone {@code telegram-bot/} consumer never sees a row that doesn't durably exist yet),
 * consumed and actually sent by that separate service, which flips {@code sent_to_telegram} to true
 * directly once the send genuinely succeeds. This method itself always sets it false at save time —
 * delivery outcome isn't known synchronously anymore, same graceful-degradation principle as AI
 * vendor calls elsewhere in this codebase: a missing connection or a failed send never blocks the
 * notification landing in the inbox.
 */
@Service
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationTelegramQueueProducer telegramQueueProducer;
  private final ObjectMapper objectMapper;

  public NotificationService(
      NotificationRepository notificationRepository,
      NotificationTelegramQueueProducer telegramQueueProducer,
      ObjectMapper objectMapper) {
    this.notificationRepository = notificationRepository;
    this.telegramQueueProducer = telegramQueueProducer;
    this.objectMapper = objectMapper;
  }

  public void sendNotification(
      UUID userId, NotificationType type, String title, String body, Map<String, String> data) {
    // data column is jsonb — must always be a real JSON document, never a raw string (see
    // NotificationEntity's Javadoc / the lesson_plans.teacher_edited_plan incident in CLAUDE.md).
    String dataJson = toJson(data);
    UUID notificationId = UUID.randomUUID();
    Notification notification =
        new Notification(
            notificationId,
            userId,
            type,
            title,
            body,
            dataJson,
            false,
            false,
            LocalDateTime.now(),
            null);
    notificationRepository.save(NotificationEntity.fromDomain(notification));
    telegramQueueProducer.publish(notificationId, userId, title, body);
  }

  /** Deviation: no inbox endpoint is documented anywhere in academix_tz.md — flagged, not spec. */
  public List<Notification> listForUser(UUID userId) {
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(NotificationEntity::toDomain)
        .toList();
  }

  public void markRead(UUID userId, UUID notificationId) {
    Notification existing =
        notificationRepository
            .findById(notificationId)
            .map(NotificationEntity::toDomain)
            .filter(notification -> notification.userId().equals(userId))
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_NOTIFICATION_NOT_FOUND",
                        "Bildirishnoma topilmadi.",
                        "Bildirishnoma ID to'g'riligini tekshiring."));
    Notification updated =
        new Notification(
            existing.id(),
            existing.userId(),
            existing.type(),
            existing.title(),
            existing.body(),
            existing.data(),
            true,
            existing.sentToTelegram(),
            existing.createdAt(),
            LocalDateTime.now());
    notificationRepository.save(NotificationEntity.fromDomain(updated));
  }

  private String toJson(Map<String, String> data) {
    try {
      return objectMapper.writeValueAsString(data == null ? Map.of() : data);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize notification data", e);
    }
  }
}
