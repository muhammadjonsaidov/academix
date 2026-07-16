package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.DataDeletionRequest;
import uz.academixai.domain.DeletionRequestStatus;

/** JPA mapping for {@code data_deletion_requests} (backend_tdd.md §4.1 table 6). */
@Entity
@Table(name = "data_deletion_requests")
public class DataDeletionRequestEntity {

  @Id private UUID id;

  @Column(name = "school_id", nullable = false)
  private UUID schoolId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "requested_by", nullable = false)
  private UUID requestedBy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DeletionRequestStatus status;

  @Column(name = "requested_at")
  private LocalDateTime requestedAt;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  protected DataDeletionRequestEntity() {}

  public DataDeletionRequestEntity(
      UUID id,
      UUID schoolId,
      UUID studentId,
      UUID requestedBy,
      DeletionRequestStatus status,
      LocalDateTime requestedAt,
      LocalDateTime approvedAt) {
    this.id = id;
    this.schoolId = schoolId;
    this.studentId = studentId;
    this.requestedBy = requestedBy;
    this.status = status;
    this.requestedAt = requestedAt;
    this.approvedAt = approvedAt;
  }

  public static DataDeletionRequestEntity fromDomain(DataDeletionRequest domain) {
    return new DataDeletionRequestEntity(
        domain.id(),
        domain.schoolId(),
        domain.studentId(),
        domain.requestedBy(),
        domain.status(),
        domain.requestedAt(),
        domain.approvedAt());
  }

  public DataDeletionRequest toDomain() {
    return new DataDeletionRequest(
        id, schoolId, studentId, requestedBy, status, requestedAt, approvedAt);
  }

  public UUID getId() {
    return id;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public UUID getStudentId() {
    return studentId;
  }
}
