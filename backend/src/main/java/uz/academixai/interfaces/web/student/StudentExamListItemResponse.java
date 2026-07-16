package uz.academixai.interfaces.web.student;

import java.time.LocalDate;
import java.util.UUID;
import uz.academixai.application.StudentExamService.ExamListItem;

/** academix_tz.md gap-fill (see StudentExamService) — GET /student/exams. */
public record StudentExamListItemResponse(
    UUID examId,
    String subject,
    String title,
    LocalDate examDate,
    StudentExamGradeResponse myGrade) {

  public static StudentExamListItemResponse from(ExamListItem item) {
    return new StudentExamListItemResponse(
        item.exam().id(),
        item.subjectName(),
        item.exam().title(),
        item.exam().examDate(),
        StudentExamGradeResponse.from(item.grade()));
  }
}
