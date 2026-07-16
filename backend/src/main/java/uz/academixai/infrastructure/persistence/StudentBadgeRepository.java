package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentBadgeRepository extends JpaRepository<StudentBadgeEntity, UUID> {

  List<StudentBadgeEntity> findByStudentId(UUID studentId);

  boolean existsByStudentIdAndBadgeId(UUID studentId, UUID badgeId);
}
