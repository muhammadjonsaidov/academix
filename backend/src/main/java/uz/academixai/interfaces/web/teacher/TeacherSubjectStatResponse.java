package uz.academixai.interfaces.web.teacher;

import java.util.List;
import uz.academixai.learning.application.TeacherAnalyticsService.SubjectStat;

/**
 * academix_tz.md §2.3 — {@code weakAreas}/{@code strongAreas} always empty: no topic-level tracking
 * exists anywhere in this codebase (see {@code TeacherAnalyticsService}'s Javadoc).
 */
public record TeacherSubjectStatResponse(
    String subject,
    double averageScore,
    double submissionRate,
    String trend,
    List<String> weakAreas,
    List<String> strongAreas) {

  public static TeacherSubjectStatResponse from(SubjectStat stat) {
    return new TeacherSubjectStatResponse(
        stat.subject(),
        stat.averageScore(),
        stat.submissionRate(),
        stat.trend(),
        List.of(),
        List.of());
  }
}
