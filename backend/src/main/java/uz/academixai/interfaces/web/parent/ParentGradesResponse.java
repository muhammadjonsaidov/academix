package uz.academixai.interfaces.web.parent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import uz.academixai.application.ParentProgressService.GradeItem;
import uz.academixai.application.ParentProgressService.Grades;

/** academix_tz.md §2.5 — GET /parent/children/{studentId}/grades, exact top-level fields. */
public record ParentGradesResponse(List<RecentGrade> recent, Map<String, Double> bySubject) {

  public record RecentGrade(UUID submissionId, String subject, int score, int fivePointGrade) {}

  public static ParentGradesResponse from(Grades grades) {
    return new ParentGradesResponse(
        grades.recent().stream().map(ParentGradesResponse::toRecent).toList(), grades.bySubject());
  }

  private static RecentGrade toRecent(GradeItem item) {
    return new RecentGrade(
        item.submissionId(), item.subject(), item.score(), item.fivePointGrade());
  }
}
