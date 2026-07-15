package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §1.8 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.HomeworkAssignmentEntity}.
 */
public record HomeworkAssignment(
    UUID id,
    UUID schoolId,
    UUID classId,
    UUID subjectId,
    UUID teacherId,
    String title,
    String description,
    AssignmentType type,
    LocalDateTime assignedAt,
    LocalDateTime deadlineAt,
    int maxScore,
    boolean isActive,
    String syllabusReference,
    String aiGenerationPrompt) {}
