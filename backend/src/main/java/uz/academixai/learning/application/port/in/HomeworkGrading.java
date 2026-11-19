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

  /**
   * Confirms the AI score of every finished, unflagged submission of one assignment in one go.
   * Flagged work (possible copying or AI-written text) is left for an individual look. Returns how
   * many submissions were graded.
   */
  int approveClean(UUID schoolId, UUID teacherId, UUID assignmentId);
}
