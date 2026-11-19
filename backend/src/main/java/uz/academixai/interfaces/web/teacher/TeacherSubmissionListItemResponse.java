package uz.academixai.interfaces.web.teacher;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.learning.application.port.in.TeacherSubmissionQuery.SubmissionWithFeedback;
import uz.academixai.learning.domain.FivePointScale;

/** academix_tz.md §2.3 — GET /teacher/submissions list item. */
public record TeacherSubmissionListItemResponse(
    UUID submissionId,
    String studentName,
    UUID studentId,
    UUID assignmentId,
    String assignmentTitle,
    LocalDateTime submittedAt,
    boolean isLate,
    String status,
    TeacherAiFeedbackResponse aiFeedback,
    Integer suggestedGrade) {

  public static TeacherSubmissionListItemResponse from(SubmissionWithFeedback item) {
    var submission = item.submission();
    return new TeacherSubmissionListItemResponse(
        submission.id(),
        item.studentName(),
        submission.studentId(),
        submission.assignmentId(),
        item.assignmentTitle(),
        submission.submittedAt(),
        submission.isLate(),
        submission.status().name(),
        TeacherAiFeedbackResponse.from(item.feedback()),
        item.feedback() == null
            ? null
            : FivePointScale.fromPercent(item.feedback().aiScorePercent()));
  }
}
