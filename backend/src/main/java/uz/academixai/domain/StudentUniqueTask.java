package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §1.9 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.StudentUniqueTaskEntity}.
 */
public record StudentUniqueTask(
    UUID id,
    UUID assignmentId,
    UUID studentId,
    String taskContent,
    boolean teacherApproved,
    boolean flaggedForReview,
    boolean fallbackToStandard,
    LocalDateTime generatedAt,
    LocalDateTime approvedAt) {}
