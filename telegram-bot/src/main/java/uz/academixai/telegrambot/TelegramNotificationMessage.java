package uz.academixai.telegrambot;

import java.util.UUID;

/**
 * Wire shape published by backend's {@code NotificationTelegramQueueProducer} onto {@code
 * notifications.telegram.queue} — kept intentionally minimal (no notification type/data), this
 * service only needs enough to resolve a chat and send plain text.
 */
public record TelegramNotificationMessage(
    UUID notificationId, UUID userId, String title, String body) {}
