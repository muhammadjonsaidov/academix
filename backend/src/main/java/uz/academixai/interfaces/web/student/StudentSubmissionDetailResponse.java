package uz.academixai.interfaces.web.student;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.application.StudentSubmissionService.StudentSubmissionDetail;

/** academix_tz.md §2.4 — GET /student/submissions/{submissionId}. */
public record StudentSubmissionDetailResponse(
    UUID submissionId,
    UUID assignmentId,
    String status,
    boolean isLate,
    LocalDateTime submittedAt,
    StudentAiFeedbackResponse aiFeedback,
    StudentGradeResponse grade) {

  public static StudentSubmissionDetailResponse from(StudentSubmissionDetail detail) {
    var submission = detail.submission();
    return new StudentSubmissionDetailResponse(
        submission.id(),
        submission.assignmentId(),
        submission.status().name(),
        submission.isLate(),
        submission.submittedAt(),
        StudentAiFeedbackResponse.from(detail.feedback()),
        StudentGradeResponse.from(detail.grade()));
  }
}
