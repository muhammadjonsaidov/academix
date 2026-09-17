package uz.academixai.interfaces.web.teacher;

import java.time.LocalDate;
import java.util.UUID;
import uz.academixai.learning.application.port.in.ExamManagement.ExamSummary;

/**
 * academix_tz.md gap-fill (deviation, judgment call, see CLAUDE.md): the spec never documents a
 * {@code GET /teacher/exams} list endpoint, only the {@code submissions} sub-resource — without
 * one, a teacher has no way to see/navigate to exams they already created.
 */
public record ExamListItemResponse(
    UUID examId,
    UUID classId,
    UUID subjectId,
    String title,
    LocalDate examDate,
    int maxScore,
    int submissionsCount,
    int gradedCount) {

  public static ExamListItemResponse from(ExamSummary summary) {
    return new ExamListItemResponse(
        summary.exam().id(),
        summary.exam().classId(),
        summary.exam().subjectId(),
        summary.exam().title(),
        summary.exam().examDate(),
        summary.exam().maxScore(),
        summary.submissionsCount(),
        summary.gradedCount());
  }
}
