package uz.academixai.interfaces.web.teacher;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.learning.application.port.in.ExamSubmissionWorkflow.SubmissionWithFeedback;

/** academix_tz.md §2.3 — GET /teacher/exams/{examId}/submissions. */
public record ExamSubmissionResponse(
    UUID submissionId,
    UUID studentId,
    String studentName,
    LocalDateTime uploadedAt,
    String status,
    boolean flaggedForReview,
    ExamAiFeedbackResponse aiFeedback,
    Integer score,
    Integer fivePointGrade,
    String teacherComment) {

  public static ExamSubmissionResponse from(SubmissionWithFeedback item) {
    var submission = item.submission();
    var grade = item.grade();
    return new ExamSubmissionResponse(
        submission.id(),
        submission.studentId(),
        item.studentName(),
        submission.uploadedAt(),
        submission.status().name(),
        submission.flaggedForReview(),
        ExamAiFeedbackResponse.from(item.feedback()),
        grade == null ? null : grade.score(),
        grade == null ? null : grade.fivePointGrade(),
        grade == null ? null : grade.teacherComment());
  }
}
