package uz.academixai.interfaces.web.teacher;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.learning.application.port.in.TeacherSubmissionQuery.SubmissionWithFeedback;
import uz.academixai.learning.domain.FivePointScale;

/** academix_tz.md §2.3 — GET /teacher/submissions/{submissionId}. */
public record TeacherSubmissionDetailResponse(
    UUID submissionId,
    UUID studentId,
    String studentName,
    String assignmentTitle,
    LocalDateTime submittedAt,
    boolean isLate,
    String status,
    String originalImageUrl,
    TeacherAiFeedbackResponse aiFeedback,
    TeacherGradeResponse previousGrade,
    Integer suggestedGrade) {

  public static TeacherSubmissionDetailResponse from(SubmissionWithFeedback item) {
    var submission = item.submission();
    return new TeacherSubmissionDetailResponse(
        submission.id(),
        submission.studentId(),
        item.studentName(),
        item.assignmentTitle(),
        submission.submittedAt(),
        submission.isLate(),
        submission.status().name(),
        submission.imageUrl(),
        TeacherAiFeedbackResponse.from(item.feedback()),
        TeacherGradeResponse.from(item.grade()),
        item.feedback() == null
            ? null
            : FivePointScale.fromPercent(item.feedback().aiScorePercent()));
  }
}
