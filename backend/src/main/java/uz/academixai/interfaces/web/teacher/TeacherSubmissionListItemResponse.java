package uz.academixai.interfaces.web.teacher;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.application.TeacherSubmissionService.SubmissionWithFeedback;

/** academix_tz.md §2.3 — GET /teacher/submissions list item. */
public record TeacherSubmissionListItemResponse(
    UUID submissionId,
    String studentName,
    UUID studentId,
    LocalDateTime submittedAt,
    boolean isLate,
    String status,
    TeacherAiFeedbackResponse aiFeedback) {

  public static TeacherSubmissionListItemResponse from(SubmissionWithFeedback item) {
    var submission = item.submission();
    return new TeacherSubmissionListItemResponse(
        submission.id(),
        item.studentName(),
        submission.studentId(),
        submission.submittedAt(),
        submission.isLate(),
        submission.status().name(),
        TeacherAiFeedbackResponse.from(item.feedback()));
  }
}
