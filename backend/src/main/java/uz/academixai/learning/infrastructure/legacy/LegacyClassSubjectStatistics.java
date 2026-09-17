package uz.academixai.learning.infrastructure.legacy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.learning.application.port.out.ClassSubjectStatistics;

/** Adapter for {@link ClassSubjectStatistics} over the legacy {@code GradeRepository}. */
@Component
public class LegacyClassSubjectStatistics implements ClassSubjectStatistics {

  private final GradeRepository grades;

  public LegacyClassSubjectStatistics(GradeRepository grades) {
    this.grades = grades;
  }

  @Override
  public List<StudentRow> classStudentProgress(UUID schoolId, UUID classId, LocalDateTime since) {
    return grades.classStudentProgress(schoolId, classId, since).stream()
        .map(
            row ->
                new StudentRow(
                    row.getStudentId(),
                    row.getFirstName(),
                    row.getLastName(),
                    row.getAvgScore(),
                    row.getGradedCount(),
                    row.getTotalXp()))
        .toList();
  }

  @Override
  public List<SubjectRow> classSubjectStats(
      UUID schoolId, UUID classId, UUID studentId, LocalDateTime since, LocalDateTime until) {
    return grades.classSubjectStats(schoolId, classId, studentId, since, until).stream()
        .map(
            row ->
                new SubjectRow(
                    row.getSubjectName(),
                    row.getAvgScore(),
                    row.getGradedCount(),
                    row.getAssignedCount(),
                    row.getSubmittedCount()))
        .toList();
  }
}
