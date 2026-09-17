package uz.academixai.notification.application.port.out;

import java.util.UUID;

/**
 * Outbound port for the async Telegram delivery path.
 *
 * <p>The request is committed with the notification itself through the transactional outbox, so a
 * rolled-back notification never produces a delivered message and a delivered message always has an
 * inbox row behind it.
 */
public interface TelegramDeliveryQueue {

  void enqueue(UUID notificationId, UUID userId, String title, String body);
}
