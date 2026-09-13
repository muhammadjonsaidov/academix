package uz.academixai.infrastructure.queue;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.outbox.OutboxService;

/**
 * Publishes to {@code notifications.telegram.queue} — the async delivery path that replaced {@code
 * NotificationService's old synchronous inline {@code TelegramClient.sendMessage} call. The
 * delivery request is committed with the notification itself through the transactional outbox.
 *
 * <p>The outbox dispatcher sets the fixed {@code telegramNotification} type-id during external
 * delivery. The bot owns an equivalent wire DTO in a different package, so FQCN type mapping is
 * deliberately not used across the service boundary.
 */
@Component
public class NotificationTelegramQueueProducer {

  private final OutboxService outbox;

  public NotificationTelegramQueueProducer(OutboxService outbox) {
    this.outbox = outbox;
  }

  public void publish(UUID notificationId, UUID userId, String title, String body) {
    outbox.enqueue(
        null,
        "Notification",
        notificationId,
        "TelegramNotificationRequested",
        NotificationTelegramQueueConfig.NOTIFICATIONS_QUEUE,
        "telegramNotification",
        new TelegramNotificationMessage(notificationId, userId, title, body));
  }
}
