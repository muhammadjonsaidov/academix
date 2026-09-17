package uz.academixai.reporting.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Outbound port for the grade-derived comparison views on the admin analytics dashboard.
 *
 * <p>Each row type is Reporting's own record. The legacy query used to hand its persistence
 * projections straight to the use case and on to the response DTOs, which is what kept
 * AdminAnalyticsService in the legacy application tree and its three response records shaped by
 * someone else's storage layout.
 */
public interface AnalyticsStatistics {

  record ClassRow(
      UUID classId, String className, int studentCount, double avgScore, long gradedCount) {}

  record TeacherRow(
      UUID teacherId, String firstName, String lastName, double avgGrade, long gradedCount) {}

  record PeriodRow(LocalDateTime periodStart, double avgScore, long gradedCount) {}

  /** Per-class averages over a window, optionally narrowed to one subject. */
  List<ClassRow> classComparison(UUID schoolId, UUID subjectId, LocalDateTime since);

  List<TeacherRow> teacherRanking(UUID schoolId);

  /** Month-bucketed school trend over a window; see the legacy query's own month bucketing. */
  List<PeriodRow> schoolProgress(UUID schoolId, LocalDateTime since);
}
