package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §1.22 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.ExamSubmissionEntity}.
 */
public record ExamSubmission(
    UUID id,
    UUID schoolId,
    UUID examId,
    UUID studentId,
    String imageUrl,
    SubmissionStatus status,
    boolean flaggedForReview,
    LocalDateTime uploadedAt) {}
