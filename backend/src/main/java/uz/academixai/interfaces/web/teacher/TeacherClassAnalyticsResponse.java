package uz.academixai.interfaces.web.teacher;

import java.util.List;
import java.util.UUID;
import uz.academixai.application.TeacherAnalyticsService.ClassAnalytics;
import uz.academixai.infrastructure.persistence.GradeRepository.StudentProgressRow;

/** academix_tz.md §2.3 {@code GET /teacher/classes/{classId}/analytics} — exact response shape. */
public record TeacherClassAnalyticsResponse(
    double classAverage,
    List<StudentSummary> topStudents,
    List<StudentSummary> bottomStudents,
    List<String> subjectWeakAreas,
    List<TeacherSubjectStatResponse> submissionRateBySubject) {

  public record StudentSummary(
      UUID studentId, String firstName, String lastName, double avgScore, long gradedCount) {

    static StudentSummary from(StudentProgressRow row) {
      return new StudentSummary(
          row.getStudentId(),
          row.getFirstName(),
          row.getLastName(),
          Math.round(row.getAvgScore() * 10) / 10.0,
          row.getGradedCount());
    }
  }

  public static TeacherClassAnalyticsResponse from(ClassAnalytics analytics) {
    return new TeacherClassAnalyticsResponse(
        analytics.classAverage(),
        analytics.topStudents().stream().map(StudentSummary::from).toList(),
        analytics.bottomStudents().stream().map(StudentSummary::from).toList(),
        analytics.subjectWeakAreas(),
        analytics.submissionRateBySubject().stream()
            .map(TeacherSubjectStatResponse::from)
            .toList());
  }
}
