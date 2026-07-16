package uz.academixai.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandwritingProfileRepository
    extends JpaRepository<HandwritingProfileEntity, UUID> {

  Optional<HandwritingProfileEntity> findByStudentId(UUID studentId);
}
