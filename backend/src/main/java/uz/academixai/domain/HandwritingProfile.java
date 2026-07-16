package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §1.13 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.HandwritingProfileEntity} (excluding {@code featureVector} — the
 * pgvector column isn't Hibernate-mappable in this stack, handled via native queries, see {@code
 * HandwritingService}). {@code featureVector} is null until the first sample and again right after
 * a reset.
 */
public record HandwritingProfile(
    UUID id,
    UUID studentId,
    int samplesCount,
    boolean isReliable,
    float[] featureVector,
    String profileVersion,
    LocalDateTime lastUpdatedAt,
    UUID lastResetByTeacherId,
    ResetReason lastResetReason,
    LocalDateTime lastResetAt,
    int resetCountThisSemester) {}
