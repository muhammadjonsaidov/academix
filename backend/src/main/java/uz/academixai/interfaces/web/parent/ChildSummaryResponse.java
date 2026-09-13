package uz.academixai.interfaces.web.parent;

import java.util.UUID;
import uz.academixai.application.ParentDashboardService.ChildSummary;
import uz.academixai.progress.application.port.in.StudentDashboard.RecentGrade;

public record ChildSummaryResponse(
    UUID studentId,
    String name,
    String className,
    boolean todayActivity,
    int pendingHomeworkCount,
    RecentGradeResponse recentGrade,
    boolean biometricConsentGiven) {

  public record RecentGradeResponse(UUID submissionId, int score, int fivePointGrade) {
    public static RecentGradeResponse from(RecentGrade grade) {
      if (grade == null) {
        return null;
      }
      return new RecentGradeResponse(grade.submissionId(), grade.score(), grade.fivePointGrade());
    }
  }

  public static ChildSummaryResponse from(ChildSummary summary) {
    return new ChildSummaryResponse(
        summary.studentId(),
        summary.name(),
        summary.className(),
        summary.todayActivity(),
        summary.pendingHomeworkCount(),
        RecentGradeResponse.from(summary.recentGrade()),
        summary.biometricConsentGiven());
  }
}
