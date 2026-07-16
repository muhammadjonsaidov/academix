package uz.academixai.interfaces.web.admin;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.DataDeletionRequest;

public record DataDeletionRequestResponse(
    UUID id,
    UUID studentId,
    UUID requestedBy,
    String status,
    LocalDateTime requestedAt,
    LocalDateTime approvedAt) {

  public static DataDeletionRequestResponse from(DataDeletionRequest domain) {
    return new DataDeletionRequestResponse(
        domain.id(),
        domain.studentId(),
        domain.requestedBy(),
        domain.status().name(),
        domain.requestedAt(),
        domain.approvedAt());
  }
}
