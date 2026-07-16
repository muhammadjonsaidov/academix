package uz.academixai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Notification;
import uz.academixai.domain.NotificationType;
import uz.academixai.infrastructure.persistence.NotificationEntity;
import uz.academixai.infrastructure.persistence.NotificationRepository;
import uz.academixai.infrastructure.persistence.TelegramConnectionRepository;
import uz.academixai.infrastructure.telegram.TelegramClient;
import uz.academixai.interfaces.web.ApiException;

/**
 * academix_tz.md §1.15 / §4 {@code sendNotification(userId, type, params)}. Always persists a
 * {@link Notification} row (inbox is the source of truth); Telegram delivery (Sprint 9) is
 * best-effort on top — looked up via {@link TelegramConnectionRepository}, sent via {@link
 * TelegramClient}, and {@code sentToTelegram} reflects whether the send actually succeeded, not
 * just whether a connection existed. No connection, or a failed send (e.g. no bot token configured
 * in dev — see {@code TelegramClient}), both leave {@code sentToTelegram=false} and the row still
 * lands correctly in the inbox — this is the same graceful-degradation principle used for AI vendor
 * calls elsewhere in this codebase, never a hard failure of the notification itself.
 */
@Service
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final TelegramConnectionRepository telegramConnectionRepository;
  private final TelegramClient telegramClient;
  private final ObjectMapper objectMapper;

  public NotificationService(
      NotificationRepository notificationRepository,
      TelegramConnectionRepository telegramConnectionRepository,
      TelegramClient telegramClient,
      ObjectMapper objectMapper) {
    this.notificationRepository = notificationRepository;
    this.telegramConnectionRepository = telegramConnectionRepository;
    this.telegramClient = telegramClient;
    this.objectMapper = objectMapper;
  }

  public void sendNotification(
      UUID userId, NotificationType type, String title, String body, Map<String, String> data) {
    // data column is jsonb — must always be a real JSON document, never a raw string (see
    // NotificationEntity's Javadoc / the lesson_plans.teacher_edited_plan incident in CLAUDE.md).
    String dataJson = toJson(data);
    boolean sentToTelegram = attemptTelegramDelivery(userId, title, body);
    Notification notification =
        new Notification(
            UUID.randomUUID(),
            userId,
            type,
            title,
            body,
            dataJson,
            false,
            sentToTelegram,
            LocalDateTime.now(),
            null);
    notificationRepository.save(NotificationEntity.fromDomain(notification));
  }

  private boolean attemptTelegramDelivery(UUID userId, String title, String body) {
    return telegramConnectionRepository
        .findByUserId(userId)
        .filter(connection -> connection.toDomain().isActive())
        .map(
            connection ->
                telegramClient.sendMessage(
                    connection.toDomain().telegramChatId(), title + "\n\n" + body))
        .orElse(false);
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

  private String toJson(Map<String, String> data) {
    try {
      return objectMapper.writeValueAsString(data == null ? Map.of() : data);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize notification data", e);
    }
  }
}
