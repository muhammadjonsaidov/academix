package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/** Deviation, judgment call — see V32 migration. Plain domain object, no framework annotations. */
public record WatchlistEntry(
    UUID id, UUID studentId, UUID addedBy, String reason, LocalDateTime addedAt) {}
