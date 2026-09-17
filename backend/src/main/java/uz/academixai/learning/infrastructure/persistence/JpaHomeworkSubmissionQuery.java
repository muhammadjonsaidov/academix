package uz.academixai.learning.infrastructure.persistence;

import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.learning.application.port.out.HomeworkSubmissionQuery;

/** JPA read adapter for assignment deletion safeguards. */
@Repository
public class JpaHomeworkSubmissionQuery implements HomeworkSubmissionQuery {

  private final HomeworkSubmissionRepository repository;

  public JpaHomeworkSubmissionQuery(HomeworkSubmissionRepository repository) {
    this.repository = repository;
  }

  @Override
  public long countByAssignmentId(UUID assignmentId) {
    return repository.countByAssignmentId(assignmentId);
  }

  @Override
  public int countPendingGradeByTeacher(UUID schoolId, UUID teacherId) {
    return repository.countPendingGradeByTeacher(schoolId, teacherId);
  }
}
