package uz.academixai.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import uz.academixai.notification.domain.NotificationPreference;
import uz.academixai.notification.domain.NotificationType;

/** JPA mapping for {@code notification_preferences} (V41). Maps to/from the domain record. */
@Entity
@Table(name = "notification_preferences")
public class NotificationPreferenceEntity {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationType type;

  @Column(name = "in_app_enabled", nullable = false)
  private boolean inAppEnabled = true;

  @Column(name = "telegram_enabled", nullable = false)
  private boolean telegramEnabled = true;

  protected NotificationPreferenceEntity() {}

  public NotificationPreferenceEntity(
      UUID id, UUID userId, NotificationType type, boolean inAppEnabled, boolean telegramEnabled) {
    this.id = id;
    this.userId = userId;
    this.type = type;
    this.inAppEnabled = inAppEnabled;
    this.telegramEnabled = telegramEnabled;
  }

  public static NotificationPreferenceEntity fromDomain(NotificationPreference domain) {
    return new NotificationPreferenceEntity(
        domain.id(),
        domain.userId(),
        domain.type(),
        domain.inAppEnabled(),
        domain.telegramEnabled());
  }

  public NotificationPreference toDomain() {
    return new NotificationPreference(id, userId, type, inAppEnabled, telegramEnabled);
  }

  public UUID getId() {
    return id;
  }

  public NotificationType getType() {
    return type;
  }

  public boolean isInAppEnabled() {
    return inAppEnabled;
  }

  public boolean isTelegramEnabled() {
    return telegramEnabled;
  }
}
