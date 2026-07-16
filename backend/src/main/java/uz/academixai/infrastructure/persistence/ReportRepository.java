package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<ReportEntity, UUID> {

  List<ReportEntity> findBySchoolIdOrderByGeneratedAtDesc(UUID schoolId);

  Optional<ReportEntity> findByIdAndSchoolId(UUID id, UUID schoolId);
}
