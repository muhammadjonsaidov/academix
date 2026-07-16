package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §1.15 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.NotificationEntity}.
 */
public record Notification(
    UUID id,
    UUID userId,
    NotificationType type,
    String title,
    String body,
    String data,
    boolean isRead,
    boolean sentToTelegram,
    LocalDateTime createdAt,
    LocalDateTime readAt) {}
