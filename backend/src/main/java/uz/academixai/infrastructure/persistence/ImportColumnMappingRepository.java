package uz.academixai.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportColumnMappingRepository
    extends JpaRepository<ImportColumnMappingEntity, UUID> {

  Optional<ImportColumnMappingEntity> findBySchoolId(UUID schoolId);
}
