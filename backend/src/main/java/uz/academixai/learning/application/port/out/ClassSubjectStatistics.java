package uz.academixai.learning.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Outbound port for the class- and subject-level grade statistics behind the teacher analytics
 * views.
 *
 * <p>Row types are Learning's own records. The legacy query handed its persistence projections to
 * the use case and on to the response DTOs, which is why the teacher analytics response shapes were
 * shaped by someone else's SELECT list.
 */
public interface ClassSubjectStatistics {

  record StudentRow(
      UUID studentId,
      String firstName,
      String lastName,
      double avgScore,
      long gradedCount,
      int totalXp) {}

  record SubjectRow(
      String subjectName,
      double avgScore,
      long gradedCount,
      long assignedCount,
      long submittedCount) {}

  List<StudentRow> classStudentProgress(UUID schoolId, UUID classId, LocalDateTime since);

  /**
   * Per-subject stats over a window, for a whole class or — when {@code studentId} is given — for
   * one student within it.
   */
  List<SubjectRow> classSubjectStats(
      UUID schoolId, UUID classId, UUID studentId, LocalDateTime since, LocalDateTime until);
}
