package uz.academixai.learning.infrastructure.legacy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.learning.application.port.out.TeacherGradingStatistics;

/** Adapter for {@link TeacherGradingStatistics} over the legacy {@code GradeRepository}. */
@Component
public class LegacyTeacherGradingStatistics implements TeacherGradingStatistics {

  private final GradeRepository grades;

  public LegacyTeacherGradingStatistics(GradeRepository grades) {
    this.grades = grades;
  }

  @Override
  public long gradedSince(UUID teacherId, LocalDateTime since) {
    return grades.countByTeacherIdAndGradedAtAfter(teacherId, since);
  }

  @Override
  public Rating rating(UUID teacherId, LocalDateTime since, LocalDateTime until) {
    var row = grades.teacherRatingForWindow(teacherId, since, until);
    return new Rating(row.getAvgGrade(), row.getGradedCount());
  }

  @Override
  public List<ClassProgress> classProgress(UUID schoolId, LocalDateTime since) {
    // The legacy query also takes a subjectId; the dashboard never filters by subject, so the port
    // drops the parameter rather than passing null through.
    return grades.classProgress(schoolId, null, since).stream()
        .map(
            row ->
                new ClassProgress(
                    row.getClassId(),
                    row.getClassName(),
                    row.getStudentCount(),
                    row.getAvgScore(),
                    row.getGradedCount()))
        .toList();
  }
}
