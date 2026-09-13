package uz.academixai.learning.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.HomeworkSubmission;

/** Student-facing read boundary for homework submissions. */
public interface HomeworkSubmissionReadRepository {

  Optional<HomeworkSubmission> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);

  List<HomeworkSubmission> findByStudentId(UUID studentId);

  Optional<HomeworkSubmission> findByIdAndSchoolId(UUID submissionId, UUID schoolId);

  List<HomeworkSubmission> findByAssignmentId(UUID assignmentId);

  List<HomeworkSubmission> findByAssignmentIds(List<UUID> assignmentIds);
}
