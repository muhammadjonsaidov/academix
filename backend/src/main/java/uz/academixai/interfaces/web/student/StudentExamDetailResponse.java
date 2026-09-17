package uz.academixai.interfaces.web.student;

import java.time.LocalDate;
import java.util.UUID;
import uz.academixai.learning.application.port.in.StudentExamQuery.ExamDetail;

/** academix_tz.md gap-fill — GET /student/exams/{examId}. */
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
        detail.submission().status().name(),
        StudentExamAiFeedbackResponse.from(detail.feedback()),
        StudentExamGradeResponse.from(detail.grade()));
  }
}
