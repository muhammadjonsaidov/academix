package uz.academixai.interfaces.web.student;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.HomeworkSubmission;

/** academix_tz.md §2.4 — GET /student/submissions history list item. */
public record StudentSubmissionListItemResponse(
    UUID submissionId,
    UUID assignmentId,
    String status,
    boolean isLate,
    LocalDateTime submittedAt) {

  public static StudentSubmissionListItemResponse from(HomeworkSubmission domain) {
    return new StudentSubmissionListItemResponse(
        domain.id(),
        domain.assignmentId(),
        domain.status().name(),
        domain.isLate(),
        domain.submittedAt());
  }
}
