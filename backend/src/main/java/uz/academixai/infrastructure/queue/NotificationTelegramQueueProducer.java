package uz.academixai.infrastructure.queue;

import java.util.UUID;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes to {@code notifications.telegram.queue} — the async delivery path that replaced {@code
 * NotificationService}'s old synchronous inline {@code TelegramClient.sendMessage} call. Same
 * afterCommit-deferral pattern as {@link HomeworkSubmissionQueueProducer} — {@code
 * NotificationService.sendNotification} can run inside an active transaction (an HTTP request via
 * {@code RlsTransactionFilter}, or {@code PsychologyService}'s own explicit {@code
 * TransactionTemplate} block), and publishing before that transaction commits would let a fast
 * consumer look up a {@code notifications} row that doesn't durably exist yet.
 *
 * <p>Explicitly sets the {@code __TypeId__} header to a fixed string rather than relying on the
 * shared {@code jsonMessageConverter} bean's default (fully-qualified-class-name) type mapping —
 * this backend's {@code TelegramNotificationMessage} and telegram-bot/'s own copy live in different
 * packages (no shared module between the two Gradle projects), so FQCN-based mapping would fail to
 * resolve on the consumer side.
 */
@Component
public class NotificationTelegramQueueProducer {

  private static final String TYPE_ID = "telegramNotification";

  private final RabbitTemplate rabbitTemplate;

  public NotificationTelegramQueueProducer(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publish(UUID notificationId, UUID userId, String title, String body) {
    TelegramNotificationMessage message =
        new TelegramNotificationMessage(notificationId, userId, title, body);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              doPublish(message);
            }
          });
    } else {
      doPublish(message);
    }
  }

  private void doPublish(TelegramNotificationMessage message) {
    MessagePostProcessor typeIdOverride =
        (Message amqpMessage) -> {
          amqpMessage.getMessageProperties().setHeader("__TypeId__", TYPE_ID);
          return amqpMessage;
        };
    rabbitTemplate.convertAndSend(
        NotificationTelegramQueueConfig.NOTIFICATIONS_QUEUE, message, typeIdOverride);
  }
}
