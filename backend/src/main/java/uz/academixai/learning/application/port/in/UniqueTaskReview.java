package uz.academixai.learning.application.port.in;

import java.util.List;
import java.util.UUID;
import uz.academixai.domain.StudentUniqueTask;

/** Published Learning use cases for reviewing generated per-student tasks. */
public interface UniqueTaskReview {

  record TaskWithStudent(StudentUniqueTask task, String studentName) {}

  List<TaskWithStudent> list(UUID schoolId, UUID teacherId, UUID assignmentId);

  StudentUniqueTask approve(UUID schoolId, UUID teacherId, UUID assignmentId, UUID taskId);

  StudentUniqueTask editContent(
      UUID schoolId, UUID teacherId, UUID assignmentId, UUID taskId, String taskContent);

  int approveAll(UUID schoolId, UUID teacherId, UUID assignmentId);

  void submit(UUID schoolId, UUID teacherId, UUID assignmentId);
}
