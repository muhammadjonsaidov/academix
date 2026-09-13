package uz.academixai.learning.application.port.in;

import java.util.UUID;
import uz.academixai.domain.Grade;

/** Published Learning command for a teacher's final homework grade. */
public interface HomeworkGrading {

  Grade grade(
      UUID schoolId,
      UUID teacherId,
      UUID submissionId,
      int score,
      int fivePointGrade,
      String teacherComment,
      boolean isExcellent);
}
