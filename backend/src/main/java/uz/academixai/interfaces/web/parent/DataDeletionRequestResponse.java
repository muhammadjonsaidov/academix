package uz.academixai.interfaces.web.parent;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.family.domain.DataDeletionRequest;

public record DataDeletionRequestResponse(
    UUID id, UUID studentId, String status, LocalDateTime requestedAt) {

  public static DataDeletionRequestResponse from(DataDeletionRequest domain) {
    return new DataDeletionRequestResponse(
        domain.id(), domain.studentId(), domain.status().name(), domain.requestedAt());
  }
}
