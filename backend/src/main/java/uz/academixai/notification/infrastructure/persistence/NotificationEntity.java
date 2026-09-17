package uz.academixai.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.academixai.notification.domain.Notification;
import uz.academixai.notification.domain.NotificationType;

/**
 * JPA mapping for {@code notifications} (see V31 migration). Maps to/from {@link Notification}.
 * {@code data} is JSON-typed like {@code PsychologicalSignalEntity.rawEvidence} — same caveat
 * applies (must be a real JSON document, not arbitrary text).
 */
@Entity
@Table(name = "notifications")
public class NotificationEntity {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationType type;

  @Column(nullable = false)
  private String title;

  private String body;

  @JdbcTypeCode(SqlTypes.JSON)
  private String data;

  @Column(name = "is_read")
  private boolean isRead;

  @Column(name = "sent_to_telegram")
  private boolean sentToTelegram;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  protected NotificationEntity() {}

  public NotificationEntity(
      UUID id,
      UUID userId,
      NotificationType type,
      String title,
      String body,
      String data,
      boolean isRead,
      boolean sentToTelegram,
      LocalDateTime createdAt,
      LocalDateTime readAt) {
    this.id = id;
    this.userId = userId;
    this.type = type;
    this.title = title;
    this.body = body;
    this.data = data;
    this.isRead = isRead;
    this.sentToTelegram = sentToTelegram;
    this.createdAt = createdAt;
    this.readAt = readAt;
  }

  public static NotificationEntity fromDomain(Notification domain) {
    return new NotificationEntity(
        domain.id(),
        domain.userId(),
        domain.type(),
        domain.title(),
        domain.body(),
        domain.data(),
        domain.isRead(),
        domain.sentToTelegram(),
        domain.createdAt(),
        domain.readAt());
  }

  public Notification toDomain() {
    return new Notification(
        id, userId, type, title, body, data, isRead, sentToTelegram, createdAt, readAt);
  }

  public UUID getUserId() {
    return userId;
  }
}
