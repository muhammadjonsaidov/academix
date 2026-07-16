package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentUniqueTaskRepository extends JpaRepository<StudentUniqueTaskEntity, UUID> {

  List<StudentUniqueTaskEntity> findByAssignmentId(UUID assignmentId);

  List<StudentUniqueTaskEntity> findByAssignmentIdAndFlaggedForReviewFalse(UUID assignmentId);

  Optional<StudentUniqueTaskEntity> findByAssignmentIdAndStudentId(
      UUID assignmentId, UUID studentId);

  Optional<StudentUniqueTaskEntity> findByIdAndAssignmentId(UUID id, UUID assignmentId);
}
