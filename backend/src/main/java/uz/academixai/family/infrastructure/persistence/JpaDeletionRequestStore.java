package uz.academixai.family.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.family.application.port.out.DeletionRequestStore;
import uz.academixai.family.domain.DataDeletionRequest;
import uz.academixai.family.domain.DeletionRequestStatus;

/** JPA adapter for Family's own {@code data_deletion_requests} table. */
@Repository
public class JpaDeletionRequestStore implements DeletionRequestStore {

  private final DataDeletionRequestRepository requests;

  public JpaDeletionRequestStore(DataDeletionRequestRepository requests) {
    this.requests = requests;
  }

  @Override
  public DataDeletionRequest save(DataDeletionRequest request) {
    return requests.save(DataDeletionRequestEntity.fromDomain(request)).toDomain();
  }

  @Override
  public Optional<DataDeletionRequest> findByIdInSchool(UUID requestId, UUID schoolId) {
    return requests
        .findByIdAndSchoolId(requestId, schoolId)
        .map(DataDeletionRequestEntity::toDomain);
  }

  @Override
  public List<DataDeletionRequest> findPendingOfSchool(UUID schoolId) {
    return requests
        .findBySchoolIdAndStatusOrderByRequestedAtDesc(schoolId, DeletionRequestStatus.PENDING)
        .stream()
        .map(DataDeletionRequestEntity::toDomain)
        .toList();
  }
}
