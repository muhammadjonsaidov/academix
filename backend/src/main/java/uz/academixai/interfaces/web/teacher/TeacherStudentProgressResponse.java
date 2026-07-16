package uz.academixai.interfaces.web.teacher;

import java.util.List;
import uz.academixai.application.TeacherAnalyticsService.StudentProgress;

/**
 * academix_tz.md §2.3 {@code GET /teacher/students/{studentId}/progress} — exact response shape.
 */
public record TeacherStudentProgressResponse(
    StudentInfoResponse student,
    List<TeacherSubjectStatResponse> subjectStats,
    List<TeacherXpHistoryItemResponse> xpHistory,
    List<RecentSubmissionResponse> recentSubmissions) {

  public static TeacherStudentProgressResponse from(StudentProgress progress) {
    return new TeacherStudentProgressResponse(
        new StudentInfoResponse(progress.studentId(), progress.firstName(), progress.lastName()),
        progress.subjectStats().stream().map(TeacherSubjectStatResponse::from).toList(),
        progress.xpHistory().stream().map(TeacherXpHistoryItemResponse::from).toList(),
        progress.recentSubmissions().stream().map(RecentSubmissionResponse::from).toList());
  }
}
