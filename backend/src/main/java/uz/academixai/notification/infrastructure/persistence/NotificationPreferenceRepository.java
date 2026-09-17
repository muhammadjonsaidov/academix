package uz.academixai.notification.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.academixai.notification.domain.NotificationType;

public interface NotificationPreferenceRepository
    extends JpaRepository<NotificationPreferenceEntity, UUID> {

  List<NotificationPreferenceEntity> findByUserId(UUID userId);

  Optional<NotificationPreferenceEntity> findByUserIdAndType(UUID userId, NotificationType type);
}
