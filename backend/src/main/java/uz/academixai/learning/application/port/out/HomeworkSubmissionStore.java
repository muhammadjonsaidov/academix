package uz.academixai.learning.application.port.out;

import java.util.UUID;
import uz.academixai.domain.HomeworkSubmission;

/** Write boundary for student homework submissions. */
public interface HomeworkSubmissionStore {

  boolean existsByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);

  HomeworkSubmission save(HomeworkSubmission submission);
}
