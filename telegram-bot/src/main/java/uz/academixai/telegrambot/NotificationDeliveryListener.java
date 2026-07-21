package uz.academixai.telegrambot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code notifications.telegram.queue} — the async delivery path that replaces backend's
 * old synchronous inline {@code TelegramClient.sendMessage} call inside {@code
 * NotificationService.sendNotification}. A thrown exception here triggers the container's retry
 * advice (3 attempts, then dead-lettered) — same shape as backend's homework/exam queue consumers.
 */
@Component
public class NotificationDeliveryListener {

  private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryListener.class);

  private final TelegramConnectionRepository connectionRepository;
  private final TelegramApiClient apiClient;
  private final NotificationStatusRepository notificationStatusRepository;

  public NotificationDeliveryListener(
      TelegramConnectionRepository connectionRepository,
      TelegramApiClient apiClient,
      NotificationStatusRepository notificationStatusRepository) {
    this.connectionRepository = connectionRepository;
    this.apiClient = apiClient;
    this.notificationStatusRepository = notificationStatusRepository;
  }

  @RabbitListener(queues = TelegramQueueConfig.NOTIFICATIONS_QUEUE)
  public void onNotification(TelegramNotificationMessage message) {
    connectionRepository
        .findActiveChatId(message.userId())
        .ifPresentOrElse(
            chatId -> {
              boolean sent =
                  apiClient.sendMessage(chatId, message.title() + "\n\n" + message.body());
              if (sent) {
                notificationStatusRepository.markSentToTelegram(message.notificationId());
              } else {
                // Real send failure (Telegram API error, network) — let the retry advice handle
                // it rather than silently swallowing, same principle as the homework consumer.
                throw new IllegalStateException(
                    "Telegram sendMessage failed for notification " + message.notificationId());
              }
            },
            () ->
                log.info(
                    "No active Telegram connection for user {}, skipping (in-app inbox already has it).",
                    message.userId()));
  }
}
