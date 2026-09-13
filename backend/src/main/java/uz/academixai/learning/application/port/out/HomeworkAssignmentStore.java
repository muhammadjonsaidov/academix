package uz.academixai.learning.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.HomeworkAssignment;

/** Persistence boundary for Learning's homework-assignment aggregate. */
public interface HomeworkAssignmentStore {

  HomeworkAssignment save(HomeworkAssignment assignment);

  List<HomeworkAssignment> findBySchoolIdAndTeacherId(UUID schoolId, UUID teacherId);

  Optional<HomeworkAssignment> findByIdAndSchoolId(UUID assignmentId, UUID schoolId);

  void delete(HomeworkAssignment assignment);
}
