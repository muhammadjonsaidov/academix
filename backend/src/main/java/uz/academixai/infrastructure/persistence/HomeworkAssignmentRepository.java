package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomeworkAssignmentRepository
    extends JpaRepository<HomeworkAssignmentEntity, UUID> {

  List<HomeworkAssignmentEntity> findBySchoolIdAndTeacherIdOrderByDeadlineAtDesc(
      UUID schoolId, UUID teacherId);

  List<HomeworkAssignmentEntity> findBySchoolIdAndClassIdOrderByDeadlineAtDesc(
      UUID schoolId, UUID classId);

  Optional<HomeworkAssignmentEntity> findByIdAndSchoolId(UUID id, UUID schoolId);
}
