package uz.academixai.learning.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Outbound port for the grade figures on a teacher's own dashboard.
 *
 * <p>Distinct from Reporting's {@code AnalyticsStatistics} on purpose even though two row shapes
 * match: that port answers the school-wide comparison an admin sees, this one answers "how am I
 * doing" for one teacher. They will diverge, and a shared type would make the first divergence a
 * breaking change for both.
 */
public interface TeacherGradingStatistics {

  record ClassProgress(
      UUID classId, String className, int studentCount, double avgScore, long gradedCount) {}

  record Rating(double avgGrade, long gradedCount) {}

  /** How many grades this teacher recorded since {@code since}. */
  long gradedSince(UUID teacherId, LocalDateTime since);

  /** This teacher's average grade over a window, with the sample size behind it. */
  Rating rating(UUID teacherId, LocalDateTime since, LocalDateTime until);

  List<ClassProgress> classProgress(UUID schoolId, LocalDateTime since);
}
