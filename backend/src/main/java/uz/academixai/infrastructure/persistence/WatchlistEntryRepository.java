package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistEntryRepository extends JpaRepository<WatchlistEntryEntity, UUID> {

  List<WatchlistEntryEntity> findAllByOrderByAddedAtDesc();

  Optional<WatchlistEntryEntity> findByStudentId(UUID studentId);

  void deleteByStudentId(UUID studentId);
}
