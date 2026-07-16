package uz.academixai.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §1.18 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.LessonPlanEntity}.
 */
public record LessonPlan(
    UUID id,
    UUID teacherId,
    UUID subjectId,
    UUID classId,
    UUID syllabusId,
    String topic,
    LessonPlanContent aiGeneratedPlan,
    String teacherEditedPlan,
    boolean isApproved,
    LocalDate lessonDate,
    LocalDateTime createdAt) {}
