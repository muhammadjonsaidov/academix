package uz.academixai.family.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §2.7 / backend_tdd.md §4.1 table 6 — plain domain object, no framework
 * annotations. JPA mapping lives in {@code infrastructure.persistence.DataDeletionRequestEntity}.
 */
public record DataDeletionRequest(
    UUID id,
    UUID schoolId,
    UUID studentId,
    UUID requestedBy,
    DeletionRequestStatus status,
    LocalDateTime requestedAt,
    LocalDateTime approvedAt) {}
