package uz.academixai.infrastructure.outbox;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select event from OutboxEventEntity event "
          + "where event.publishedAt is null and event.availableAt <= :now order by event.occurredAt")
  List<OutboxEventEntity> lockPending(@Param("now") LocalDateTime now, Pageable pageable);
}
