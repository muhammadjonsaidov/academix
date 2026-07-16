package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandwritingResetLogRepository
    extends JpaRepository<HandwritingResetLogEntity, UUID> {

  List<HandwritingResetLogEntity> findByStudentIdOrderByResetAtDesc(UUID studentId);

  List<HandwritingResetLogEntity> findBySchoolId(UUID schoolId);
}
