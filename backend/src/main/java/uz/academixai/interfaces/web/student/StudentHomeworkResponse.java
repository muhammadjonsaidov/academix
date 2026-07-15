package uz.academixai.interfaces.web.student;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.application.StudentSubmissionService.StudentHomeworkItem;

/**
 * academix_tz.md §2.4 — GET /student/homework(/{assignmentId}). {@code myTask} (per-student
 * unique-generated content) is omitted — unique-task generation is Sprint 3+ scope (every
 * assignment is STANDARD this sprint, see ROADMAP.md).
 */
public record StudentHomeworkResponse(
    UUID assignmentId,
    String subject,
    String title,
    LocalDateTime deadlineAt,
    boolean isLate,
    String submissionStatus) {

  public static StudentHomeworkResponse from(StudentHomeworkItem item) {
    return new StudentHomeworkResponse(
        item.assignmentId(),
        item.subjectName(),
        item.title(),
        item.deadlineAt(),
        item.isLate(),
        item.submissionStatus());
  }
}
