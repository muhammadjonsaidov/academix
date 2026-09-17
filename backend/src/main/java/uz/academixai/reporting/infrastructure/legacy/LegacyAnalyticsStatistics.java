package uz.academixai.reporting.infrastructure.legacy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.reporting.application.port.out.AnalyticsStatistics;

/** Adapter for {@link AnalyticsStatistics} over the legacy {@code GradeRepository}. */
@Component
public class LegacyAnalyticsStatistics implements AnalyticsStatistics {

  private final GradeRepository grades;

  public LegacyAnalyticsStatistics(GradeRepository grades) {
    this.grades = grades;
  }

  @Override
  public List<ClassRow> classComparison(UUID schoolId, UUID subjectId, LocalDateTime since) {
    return grades.classProgress(schoolId, subjectId, since).stream()
        .map(
            row ->
                new ClassRow(
                    row.getClassId(),
                    row.getClassName(),
                    row.getStudentCount(),
                    row.getAvgScore(),
                    row.getGradedCount()))
        .toList();
  }

  @Override
  public List<TeacherRow> teacherRanking(UUID schoolId) {
    return grades.teacherRanking(schoolId).stream()
        .map(
            row ->
                new TeacherRow(
                    row.getTeacherId(),
                    row.getFirstName(),
                    row.getLastName(),
                    row.getAvgGrade(),
                    row.getGradedCount()))
        .toList();
  }

  @Override
  public List<PeriodRow> schoolProgress(UUID schoolId, LocalDateTime since) {
    return grades.schoolProgress(schoolId, since).stream()
        .map(row -> new PeriodRow(row.getPeriodStart(), row.getAvgScore(), row.getGradedCount()))
        .toList();
  }
}
