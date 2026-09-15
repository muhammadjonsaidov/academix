package uz.academixai.family.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.academixai.family.domain.DeletionRequestStatus;

public interface DataDeletionRequestRepository
    extends JpaRepository<DataDeletionRequestEntity, UUID> {

  List<DataDeletionRequestEntity> findBySchoolIdAndStatusOrderByRequestedAtDesc(
      UUID schoolId, DeletionRequestStatus status);

  Optional<DataDeletionRequestEntity> findByIdAndSchoolId(UUID id, UUID schoolId);
}
