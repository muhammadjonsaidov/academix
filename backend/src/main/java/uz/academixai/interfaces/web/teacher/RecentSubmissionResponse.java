package uz.academixai.interfaces.web.teacher;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.HomeworkSubmission;

/** academix_tz.md §2.3 {@code GET /teacher/students/{id}/progress}'s {@code recentSubmissions}. */
public record RecentSubmissionResponse(
    UUID submissionId,
    UUID assignmentId,
    String status,
    boolean isLate,
    LocalDateTime submittedAt) {

  public static RecentSubmissionResponse from(HomeworkSubmission domain) {
    return new RecentSubmissionResponse(
        domain.id(),
        domain.assignmentId(),
        domain.status().name(),
        domain.isLate(),
        domain.submittedAt());
  }
}
