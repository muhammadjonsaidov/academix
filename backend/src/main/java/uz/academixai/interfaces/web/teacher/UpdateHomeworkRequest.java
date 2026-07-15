package uz.academixai.interfaces.web.teacher;

import java.time.LocalDateTime;

/** academix_tz.md §2.3 — PUT /teacher/homework/{assignmentId} body. */
public record UpdateHomeworkRequest(
    String title,
    String description,
    LocalDateTime deadlineAt,
    String syllabusReference,
    int maxScore) {}
