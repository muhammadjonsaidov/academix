package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §2.4 "GET /student/xp-history" ({date, xp, reason}) — plain domain object, no
 * framework annotations. JPA mapping lives in {@code infrastructure.persistence.XpHistoryEntity}.
 */
public record XpHistoryEntry(
    UUID id, UUID studentId, int xp, String reason, LocalDateTime occurredAt) {}
