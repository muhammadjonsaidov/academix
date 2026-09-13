package uz.academixai.progress.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.progress.domain.XpHistoryEntry;

/** JPA mapping for {@code xp_history} (migration V22). Maps to/from {@link XpHistoryEntry}. */
@Entity
@Table(name = "xp_history")
public class XpHistoryEntity {

  @Id private UUID id;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(nullable = false)
  private int xp;

  @Column(nullable = false)
  private String reason;

  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  protected XpHistoryEntity() {}

  public XpHistoryEntity(UUID id, UUID studentId, int xp, String reason, LocalDateTime occurredAt) {
    this.id = id;
    this.studentId = studentId;
    this.xp = xp;
    this.reason = reason;
    this.occurredAt = occurredAt;
  }

  public static XpHistoryEntity fromDomain(XpHistoryEntry domain) {
    return new XpHistoryEntity(
        domain.id(), domain.studentId(), domain.xp(), domain.reason(), domain.occurredAt());
  }

  public XpHistoryEntry toDomain() {
    return new XpHistoryEntry(id, studentId, xp, reason, occurredAt);
  }
}
