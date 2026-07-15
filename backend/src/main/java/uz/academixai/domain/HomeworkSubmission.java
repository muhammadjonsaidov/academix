package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §1.10 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.HomeworkSubmissionEntity}. {@code studentTaskId} is always null this
 * sprint — every assignment is {@code STANDARD} (unique-task generation is Sprint 3+ scope, see
 * ROADMAP.md).
 */
public record HomeworkSubmission(
    UUID id,
    UUID schoolId,
    UUID assignmentId,
    UUID studentTaskId,
    UUID studentId,
    SubmissionType type,
    String textContent,
    String imageUrl,
    SubmissionStatus status,
    boolean isLate,
    LocalDateTime submittedAt,
    int xpEarned) {}
