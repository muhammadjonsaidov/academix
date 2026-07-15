package uz.academixai.interfaces.web.teacher;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.AssignmentType;

/**
 * academix_tz.md §2.3 — { classId, subjectId, title, description, deadlineAt, type,
 * syllabusReference?, maxScore }
 */
public record CreateHomeworkRequest(
    UUID classId,
    UUID subjectId,
    String title,
    String description,
    LocalDateTime deadlineAt,
    AssignmentType type,
    String syllabusReference,
    int maxScore) {}
