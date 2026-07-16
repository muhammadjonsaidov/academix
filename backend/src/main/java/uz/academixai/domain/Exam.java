package uz.academixai.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §1.21 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.ExamEntity}.
 */
public record Exam(
    UUID id,
    UUID schoolId,
    UUID classId,
    UUID subjectId,
    UUID teacherId,
    String title,
    LocalDate examDate,
    int maxScore,
    LocalDateTime createdAt) {}
