package uz.academixai.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.notification.domain.TelegramConnection;

/** JPA mapping for {@code telegram_connections} (backend_tdd.md §4.1 table 15). */
@Entity
@Table(name = "telegram_connections")
public class TelegramConnectionEntity {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false, unique = true)
  private UUID userId;

  @Column(name = "telegram_chat_id", nullable = false)
  private long telegramChatId;

  @Column(name = "telegram_username")
  private String telegramUsername;

  @Column(name = "is_active")
  private boolean isActive;

  @Column(name = "connected_at")
  private LocalDateTime connectedAt;

  protected TelegramConnectionEntity() {}

  public TelegramConnectionEntity(
      UUID id,
      UUID userId,
      long telegramChatId,
      String telegramUsername,
      boolean isActive,
      LocalDateTime connectedAt) {
    this.id = id;
    this.userId = userId;
    this.telegramChatId = telegramChatId;
    this.telegramUsername = telegramUsername;
    this.isActive = isActive;
    this.connectedAt = connectedAt;
  }

  public static TelegramConnectionEntity fromDomain(TelegramConnection domain) {
    return new TelegramConnectionEntity(
        domain.id(),
        domain.userId(),
        domain.telegramChatId(),
        domain.telegramUsername(),
        domain.isActive(),
        domain.connectedAt());
  }

  public TelegramConnection toDomain() {
    return new TelegramConnection(
        id, userId, telegramChatId, telegramUsername, isActive, connectedAt);
  }

  public UUID getUserId() {
    return userId;
  }
}
