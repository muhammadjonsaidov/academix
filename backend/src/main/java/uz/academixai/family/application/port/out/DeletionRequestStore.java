package uz.academixai.family.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.family.domain.DataDeletionRequest;

/** Persistence port for data-deletion requests — Family owns {@code data_deletion_requests}. */
public interface DeletionRequestStore {

  DataDeletionRequest save(DataDeletionRequest request);

  Optional<DataDeletionRequest> findByIdInSchool(UUID requestId, UUID schoolId);

  List<DataDeletionRequest> findPendingOfSchool(UUID schoolId);
}
