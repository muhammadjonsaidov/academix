package uz.academixai.learning.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.StudentUniqueTask;

/** Persistence boundary for per-student unique tasks. */
public interface StudentUniqueTaskStore {

  List<StudentUniqueTask> findByAssignmentId(UUID assignmentId);

  List<StudentUniqueTask> findUnflaggedByAssignmentId(UUID assignmentId);

  Optional<StudentUniqueTask> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);

  Optional<StudentUniqueTask> findByIdAndAssignmentId(UUID taskId, UUID assignmentId);

  StudentUniqueTask save(StudentUniqueTask task);
}
