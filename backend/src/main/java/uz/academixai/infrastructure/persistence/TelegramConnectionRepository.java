package uz.academixai.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramConnectionRepository
    extends JpaRepository<TelegramConnectionEntity, UUID> {

  Optional<TelegramConnectionEntity> findByUserId(UUID userId);
}
