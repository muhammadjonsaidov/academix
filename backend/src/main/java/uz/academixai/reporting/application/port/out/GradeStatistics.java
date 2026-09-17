package uz.academixai.reporting.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Outbound port for the aggregate grade figures a report is built from.
 *
 * <p>The row types are Reporting's own: the legacy query returned its persistence projections
 * straight into the application layer, which is what tied {@code ReportService} to {@code
 * infrastructure.persistence}. A report only needs these three numbers per row, so the port states
 * exactly that.
 */
public interface GradeStatistics {

  record ClassRow(String className, double avgScore, long gradedCount, int studentCount) {}

  record StudentRow(
      String firstName, String lastName, double avgScore, long gradedCount, int totalXp) {}

  record SubjectRow(String subjectName, double avgScore, long gradedCount) {}

  List<ClassRow> classProgress(UUID schoolId, LocalDateTime since);

  List<StudentRow> classStudentProgress(UUID schoolId, UUID classId, LocalDateTime since);

  List<SubjectRow> studentSubjectProgress(UUID schoolId, UUID studentId, LocalDateTime since);
}
