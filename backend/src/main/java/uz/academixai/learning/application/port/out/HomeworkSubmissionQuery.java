package uz.academixai.learning.application.port.out;

import java.util.UUID;

/** Read-only dependency needed before a homework aggregate may be deleted. */
public interface HomeworkSubmissionQuery {

  long countByAssignmentId(UUID assignmentId);
}
