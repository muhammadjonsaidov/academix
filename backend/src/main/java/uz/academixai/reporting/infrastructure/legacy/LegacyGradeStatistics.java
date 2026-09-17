package uz.academixai.reporting.infrastructure.legacy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.reporting.application.port.out.GradeStatistics;

/** Adapter for {@link GradeStatistics} over the legacy {@code GradeRepository}. */
@Component
public class LegacyGradeStatistics implements GradeStatistics {

  private final GradeRepository grades;

  public LegacyGradeStatistics(GradeRepository grades) {
    this.grades = grades;
  }

  @Override
  public List<ClassRow> classProgress(UUID schoolId, LocalDateTime since) {
    // The legacy query also takes a subjectId; Reporting never filters by subject, so the port
    // drops the parameter rather than passing null through.
    return grades.classProgress(schoolId, null, since).stream()
        .map(
            row ->
                new ClassRow(
                    row.getClassName(),
                    row.getAvgScore(),
                    row.getGradedCount(),
                    row.getStudentCount()))
        .toList();
  }

  @Override
  public List<StudentRow> classStudentProgress(UUID schoolId, UUID classId, LocalDateTime since) {
    return grades.classStudentProgress(schoolId, classId, since).stream()
        .map(
            row ->
                new StudentRow(
                    row.getFirstName(),
                    row.getLastName(),
                    row.getAvgScore(),
                    row.getGradedCount(),
                    row.getTotalXp()))
        .toList();
  }

  @Override
  public List<SubjectRow> studentSubjectProgress(
      UUID schoolId, UUID studentId, LocalDateTime since) {
    return grades.studentSubjectProgress(schoolId, studentId, since).stream()
        .map(row -> new SubjectRow(row.getSubjectName(), row.getAvgScore(), row.getGradedCount()))
        .toList();
  }
}
