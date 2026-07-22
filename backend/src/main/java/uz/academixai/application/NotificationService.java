package uz.academixai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.academixai.domain.Notification;
import uz.academixai.domain.NotificationPreference;
import uz.academixai.domain.NotificationType;
import uz.academixai.infrastructure.persistence.NotificationEntity;
import uz.academixai.infrastructure.persistence.NotificationPreferenceEntity;
import uz.academixai.infrastructure.persistence.NotificationPreferenceRepository;
import uz.academixai.infrastructure.persistence.NotificationRepository;
import uz.academixai.infrastructure.queue.NotificationTelegramQueueProducer;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §1.15 / §4 {@code sendNotification(userId, type, params)}. Always persists a
 * {@link Notification} row (inbox is the source of truth); Telegram delivery is async on top —
 * enqueued via {@link NotificationTelegramQueueProducer} (only after this transaction commits, so
 * the standalone {@code telegram-bot/} consumer never sees a row that doesn't durably exist yet),
 * consumed and actually sent by that separate service, which flips {@code sent_to_telegram} to true
 * directly once the send genuinely succeeds. This method itself always sets it false at save time —
 * delivery outcome isn't known synchronously anymore, same graceful-degradation principle as AI
 * vendor calls elsewhere in this codebase: a missing connection or a failed send never blocks the
 * notification landing in the inbox.
 */
@Service
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationPreferenceRepository preferenceRepository;
  private final NotificationTelegramQueueProducer telegramQueueProducer;
  private final ObjectMapper objectMapper;

  public NotificationService(
      NotificationRepository notificationRepository,
      NotificationPreferenceRepository preferenceRepository,
      NotificationTelegramQueueProducer telegramQueueProducer,
      ObjectMapper objectMapper) {
    this.notificationRepository = notificationRepository;
    this.preferenceRepository = preferenceRepository;
    this.telegramQueueProducer = telegramQueueProducer;
    this.objectMapper = objectMapper;
  }

  public void sendNotification(
      UUID userId, NotificationType type, String title, String body, Map<String, String> data) {
    // Preference gate. PSYCHOLOGICAL_ALERT bypasses it entirely — TZ §1.14's severity→notify
    // matrix is a safety rule (CRITICAL must reach the parent), muting it isn't offered.
    // The inbox row is the prerequisite for Telegram delivery (the standalone bot consumer
    // resolves the row by id), so in-app disabled means nothing is sent on either channel —
    // the settings UI communicates that dependency.
    boolean inAppEnabled = true;
    boolean telegramEnabled = true;
    if (type != NotificationType.PSYCHOLOGICAL_ALERT) {
      var pref = preferenceRepository.findByUserIdAndType(userId, type);
      inAppEnabled = pref.map(NotificationPreferenceEntity::isInAppEnabled).orElse(true);
      telegramEnabled = pref.map(NotificationPreferenceEntity::isTelegramEnabled).orElse(true);
    }
    if (!inAppEnabled) {
      return;
    }
    // data column is jsonb — must always be a real JSON document, never a raw string (see
    // NotificationEntity's Javadoc / the lesson_plans.teacher_edited_plan incident in CLAUDE.md).
    String dataJson = toJson(data);
    UUID notificationId = UUID.randomUUID();
    Notification notification =
        new Notification(
            notificationId,
            userId,
            type,
            title,
            body,
            dataJson,
            false,
            false,
            LocalDateTime.now(),
            null);
    notificationRepository.save(NotificationEntity.fromDomain(notification));
    if (telegramEnabled) {
      telegramQueueProducer.publish(notificationId, userId, title, body);
    }
  }

  /** Deviation: no inbox endpoint is documented anywhere in academix_tz.md — flagged, not spec. */
  public List<Notification> listForUser(UUID userId) {
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(NotificationEntity::toDomain)
        .toList();
  }

  public void markRead(UUID userId, UUID notificationId) {
    Notification existing =
        notificationRepository
            .findById(notificationId)
            .map(NotificationEntity::toDomain)
            .filter(notification -> notification.userId().equals(userId))
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_NOTIFICATION_NOT_FOUND",
                        "Bildirishnoma topilmadi.",
                        "Bildirishnoma ID to'g'riligini tekshiring."));
    Notification updated =
        new Notification(
            existing.id(),
            existing.userId(),
            existing.type(),
            existing.title(),
            existing.body(),
            existing.data(),
            true,
            existing.sentToTelegram(),
            existing.createdAt(),
            LocalDateTime.now());
    notificationRepository.save(NotificationEntity.fromDomain(updated));
  }

  /** Server-side bulk mark-read (replaces the frontend's old N-requests loop). */
  @Transactional
  public void markAllRead(UUID userId) {
    notificationRepository.markAllRead(userId, LocalDateTime.now());
  }

  /**
   * Own-notification delete. {@code noRollbackFor} per the CLAUDE.md rule: this participates in
   * the request's transaction and throws a business ApiException before any write.
   */
  @Transactional(noRollbackFor = ApiException.class)
  public void delete(UUID userId, UUID notificationId) {
    int deleted = notificationRepository.deleteByIdAndUserId(notificationId, userId);
    if (deleted == 0) {
      throw new ApiException(
          HttpStatus.NOT_FOUND,
          "ERR_NOTIFICATION_NOT_FOUND",
          "Bildirishnoma topilmadi.",
          "Bildirishnoma ID to'g'riligini tekshiring.");
    }
  }

  @Transactional
  public void deleteAll(UUID userId) {
    notificationRepository.deleteAllByUserId(userId);
  }

  /**
   * Effective preferences for every {@link NotificationType} — stored rows merged over
   * default-enabled, so the settings UI always renders the full list.
   */
  public List<NotificationPreference> preferences(UUID userId) {
    Map<NotificationType, NotificationPreferenceEntity> stored =
        preferenceRepository.findByUserId(userId).stream()
            .collect(Collectors.toMap(NotificationPreferenceEntity::getType, Function.identity()));
    return java.util.Arrays.stream(NotificationType.values())
        .map(
            type -> {
              NotificationPreferenceEntity entity = stored.get(type);
              return new NotificationPreference(
                  entity != null ? entity.getId() : null,
                  userId,
                  type,
                  entity == null || entity.isInAppEnabled(),
                  entity == null || entity.isTelegramEnabled());
            })
        .toList();
  }

  /** Upsert one type's toggles. PSYCHOLOGICAL_ALERT is rejected — see sendNotification. */
  @Transactional(noRollbackFor = ApiException.class)
  public void updatePreference(
      UUID userId, NotificationType type, boolean inAppEnabled, boolean telegramEnabled) {
    if (type == NotificationType.PSYCHOLOGICAL_ALERT) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_PREFERENCE_LOCKED",
          "Psixologik signal bildirishnomalarini o'chirib bo'lmaydi.",
          "Bu tur xavfsizlik qoidasi bo'yicha doim yuboriladi.");
    }
    NotificationPreferenceEntity entity =
        preferenceRepository
            .findByUserIdAndType(userId, type)
            .map(
                existing ->
                    new NotificationPreferenceEntity(
                        existing.getId(), userId, type, inAppEnabled, telegramEnabled))
            .orElseGet(
                () ->
                    new NotificationPreferenceEntity(
                        UUID.randomUUID(), userId, type, inAppEnabled, telegramEnabled));
    preferenceRepository.save(entity);
  }

  private String toJson(Map<String, String> data) {
    try {
      return objectMapper.writeValueAsString(data == null ? Map.of() : data);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize notification data", e);
    }
  }
}
