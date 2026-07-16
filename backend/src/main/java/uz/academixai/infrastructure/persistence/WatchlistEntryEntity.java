package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.WatchlistEntry;

/** JPA mapping for {@code psychology_watchlist} (see V32 migration). */
@Entity
@Table(name = "psychology_watchlist")
public class WatchlistEntryEntity {

  @Id private UUID id;

  @Column(name = "student_id", nullable = false, unique = true)
  private UUID studentId;

  @Column(name = "added_by", nullable = false)
  private UUID addedBy;

  private String reason;

  @Column(name = "added_at")
  private LocalDateTime addedAt;

  protected WatchlistEntryEntity() {}

  public WatchlistEntryEntity(
      UUID id, UUID studentId, UUID addedBy, String reason, LocalDateTime addedAt) {
    this.id = id;
    this.studentId = studentId;
    this.addedBy = addedBy;
    this.reason = reason;
    this.addedAt = addedAt;
  }

  public static WatchlistEntryEntity fromDomain(WatchlistEntry domain) {
    return new WatchlistEntryEntity(
        domain.id(), domain.studentId(), domain.addedBy(), domain.reason(), domain.addedAt());
  }

  public WatchlistEntry toDomain() {
    return new WatchlistEntry(id, studentId, addedBy, reason, addedAt);
  }

  public UUID getStudentId() {
    return studentId;
  }
}
