package uz.academixai.learning.application.port.out;

import java.util.UUID;

/** Read-only dependency needed before a homework aggregate may be deleted. */
public interface HomeworkSubmissionQuery {

  long countByAssignmentId(UUID assignmentId);

  /** Submissions still awaiting a grade from this teacher. */
  int countPendingGradeByTeacher(UUID schoolId, UUID teacherId);
}
