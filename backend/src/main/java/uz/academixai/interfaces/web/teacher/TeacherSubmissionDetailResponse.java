package uz.academixai.interfaces.web.teacher;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.learning.application.port.in.TeacherSubmissionQuery.SubmissionWithFeedback;

/** academix_tz.md §2.3 — GET /teacher/submissions/{submissionId}. */
public record TeacherSubmissionDetailResponse(
    UUID submissionId,
    UUID studentId,
    String studentName,
    LocalDateTime submittedAt,
    boolean isLate,
    String status,
    String originalImageUrl,
    TeacherAiFeedbackResponse aiFeedback,
    TeacherGradeResponse previousGrade) {

  public static TeacherSubmissionDetailResponse from(SubmissionWithFeedback item) {
    var submission = item.submission();
    return new TeacherSubmissionDetailResponse(
        submission.id(),
        submission.studentId(),
        item.studentName(),
        submission.submittedAt(),
        submission.isLate(),
        submission.status().name(),
        submission.imageUrl(),
        TeacherAiFeedbackResponse.from(item.feedback()),
        TeacherGradeResponse.from(item.grade()));
  }
}
