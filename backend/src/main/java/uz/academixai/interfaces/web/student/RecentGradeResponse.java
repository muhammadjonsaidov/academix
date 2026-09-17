package uz.academixai.interfaces.web.student;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.progress.application.port.in.StudentDashboard.RecentGrade;

public record RecentGradeResponse(
    UUID submissionId, int score, int fivePointGrade, LocalDateTime gradedAt) {

  public static RecentGradeResponse from(RecentGrade grade) {
    return new RecentGradeResponse(
        grade.submissionId(), grade.score(), grade.fivePointGrade(), grade.gradedAt());
  }
}
