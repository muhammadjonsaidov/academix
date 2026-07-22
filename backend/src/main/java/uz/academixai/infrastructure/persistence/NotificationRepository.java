package uz.academixai.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

  List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

  @Modifying
  @Query(
      "UPDATE NotificationEntity n SET n.isRead = true, n.readAt = :now"
          + " WHERE n.userId = :userId AND n.isRead = false")
  int markAllRead(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

  @Modifying
  @Query("DELETE FROM NotificationEntity n WHERE n.id = :id AND n.userId = :userId")
  int deleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

  @Modifying
  @Query("DELETE FROM NotificationEntity n WHERE n.userId = :userId")
  int deleteAllByUserId(@Param("userId") UUID userId);
}
