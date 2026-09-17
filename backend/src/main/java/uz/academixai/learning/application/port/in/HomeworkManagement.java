package uz.academixai.learning.application.port.in;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import uz.academixai.domain.AssignmentType;
import uz.academixai.domain.HomeworkAssignment;

/** Published Learning use cases for a teacher's homework lifecycle. */
public interface HomeworkManagement {

  HomeworkAssignment create(
      UUID schoolId,
      UUID teacherId,
      UUID classId,
      UUID subjectId,
      String title,
      String description,
      AssignmentType type,
      LocalDateTime deadlineAt,
      String syllabusReference,
      int maxScore);

  List<HomeworkAssignment> list(UUID schoolId, UUID teacherId, UUID classId, UUID subjectId);

  HomeworkAssignment get(UUID schoolId, UUID assignmentId);

  HomeworkAssignment update(
      UUID schoolId,
      UUID teacherId,
      UUID assignmentId,
      String title,
      String description,
      LocalDateTime deadlineAt,
      String syllabusReference,
      int maxScore);

  void delete(UUID schoolId, UUID teacherId, UUID assignmentId);
}
