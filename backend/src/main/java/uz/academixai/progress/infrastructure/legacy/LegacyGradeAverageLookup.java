package uz.academixai.progress.infrastructure.legacy;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.persistence.GradeRepository;
import uz.academixai.progress.application.port.out.GradeAverageLookup;

/**
 * Adapter for Progress' {@code GradeAverageLookup} port over the legacy {@code GradeRepository}.
 *
 * <p>Named "legacy" for the same reason as {@link LegacyParentChildAccess}: it is the seam that
 * keeps a legacy dependency out of the application layer, and it disappears when the grade
 * repository moves into the context that owns it.
 */
@Component
public class LegacyGradeAverageLookup implements GradeAverageLookup {

  private final GradeRepository grades;

  public LegacyGradeAverageLookup(GradeRepository grades) {
    this.grades = grades;
  }

  @Override
  public double averageScore(UUID studentId, LocalDateTime since, LocalDateTime until) {
    return grades.studentAvgScoreForWindow(studentId, since, until);
  }
}
