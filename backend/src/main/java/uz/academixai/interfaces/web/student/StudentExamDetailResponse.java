package uz.academixai.interfaces.web.student;

import java.time.LocalDate;
import java.util.UUID;
import uz.academixai.application.StudentExamService.ExamDetail;

/** academix_tz.md gap-fill (see StudentExamService) — GET /student/exams/{examId}. */
public record StudentExamDetailResponse(
    UUID examId,
    String title,
    LocalDate examDate,
    String status,
    StudentExamAiFeedbackResponse aiFeedback,
    StudentExamGradeResponse grade) {

  public static StudentExamDetailResponse from(ExamDetail detail) {
    return new StudentExamDetailResponse(
        detail.exam().id(),
        detail.exam().title(),
        detail.exam().examDate(),
        detail.submission().getStatus().name(),
        StudentExamAiFeedbackResponse.from(detail.feedback()),
        StudentExamGradeResponse.from(detail.grade()));
  }
}
