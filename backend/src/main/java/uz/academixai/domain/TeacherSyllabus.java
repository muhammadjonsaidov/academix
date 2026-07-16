package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §1.17 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.TeacherSyllabusEntity}.
 */
public record TeacherSyllabus(
    UUID id,
    UUID teacherId,
    UUID subjectId,
    UUID classId,
    String title,
    String fileUrl,
    FileType fileType,
    String extractedContent,
    boolean isProcessed,
    LocalDateTime uploadedAt) {}
