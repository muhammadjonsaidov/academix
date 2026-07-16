package uz.academixai.interfaces.web.parent;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.HomeworkSubmission;

public record ParentSubmissionResponse(
    UUID submissionId,
    UUID assignmentId,
    String status,
    boolean isLate,
    LocalDateTime submittedAt) {

  public static ParentSubmissionResponse from(HomeworkSubmission domain) {
    return new ParentSubmissionResponse(
        domain.id(),
        domain.assignmentId(),
        domain.status().name(),
        domain.isLate(),
        domain.submittedAt());
  }
}
