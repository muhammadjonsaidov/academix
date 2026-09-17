package uz.academixai.notification.infrastructure.messaging;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.queue.NotificationTelegramQueueProducer;
import uz.academixai.notification.application.port.out.TelegramDeliveryQueue;

/**
 * Adapter over the legacy queue producer.
 *
 * <p>A thin delegate rather than a move: the producer still sits in the legacy infrastructure tree,
 * and the strangler step is to give the context a port it owns now. When that tree is retired the
 * producer's body moves here and this class disappears.
 */
@Component
public class TelegramDeliveryQueueAdapter implements TelegramDeliveryQueue {

  private final NotificationTelegramQueueProducer producer;

  public TelegramDeliveryQueueAdapter(NotificationTelegramQueueProducer producer) {
    this.producer = producer;
  }

  @Override
  public void enqueue(UUID notificationId, UUID userId, String title, String body) {
    producer.publish(notificationId, userId, title, body);
  }
}
