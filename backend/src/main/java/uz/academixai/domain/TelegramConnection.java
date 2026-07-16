package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §1.16 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.TelegramConnectionEntity}.
 */
public record TelegramConnection(
    UUID id,
    UUID userId,
    long telegramChatId,
    String telegramUsername,
    boolean isActive,
    LocalDateTime connectedAt) {}
