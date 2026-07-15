package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §1.12 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.GradeEntity}.
 */
public record Grade(
    UUID id,
    UUID submissionId,
    UUID teacherId,
    int score,
    int fivePointGrade,
    String teacherComment,
    boolean teacherOverrodeAI,
    float aiOriginalScore,
    LocalDateTime gradedAt) {}
