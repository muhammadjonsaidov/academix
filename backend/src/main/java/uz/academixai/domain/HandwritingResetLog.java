package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §1.13.1 — plain domain object, no framework annotations. JPA mapping lives in
 * {@code infrastructure.persistence.HandwritingResetLogEntity}.
 */
public record HandwritingResetLog(
    UUID id,
    UUID schoolId,
    UUID studentId,
    UUID teacherId,
    ResetReason reason,
    String notes,
    String previousProfileVersion,
    LocalDateTime resetAt) {}
