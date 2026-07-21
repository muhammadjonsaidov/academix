package uz.academixai.infrastructure.queue;

import java.util.UUID;

/**
 * Backend's own copy of the same wire shape telegram-bot/'s {@code TelegramNotificationMessage}
 * expects — no shared module between the two Gradle projects, each side owns its copy. Kept
 * intentionally minimal (no notification type/data), the bot only needs enough to resolve a chat
 * and send plain text.
 */
public record TelegramNotificationMessage(
    UUID notificationId, UUID userId, String title, String body) {}
