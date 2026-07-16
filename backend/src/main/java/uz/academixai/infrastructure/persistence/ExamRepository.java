package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<ExamEntity, UUID> {

  List<ExamEntity> findBySchoolIdAndTeacherIdOrderByExamDateDesc(UUID schoolId, UUID teacherId);

  List<ExamEntity> findBySchoolIdAndClassIdOrderByExamDateDesc(UUID schoolId, UUID classId);

  Optional<ExamEntity> findByIdAndSchoolId(UUID id, UUID schoolId);
}
