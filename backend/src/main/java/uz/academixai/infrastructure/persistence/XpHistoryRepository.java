package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface XpHistoryRepository extends JpaRepository<XpHistoryEntity, UUID> {

  List<XpHistoryEntity> findByStudentIdOrderByOccurredAtDesc(UUID studentId);
}
