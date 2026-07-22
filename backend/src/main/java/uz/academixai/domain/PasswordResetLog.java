package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §2.1 — audit trail for the class-teacher-assisted STUDENT password reset. Plain
 * domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.PasswordResetLogEntity}.
 */
public record PasswordResetLog(
    UUID id, UUID schoolId, UUID studentId, UUID teacherId, LocalDateTime resetAt) {}
