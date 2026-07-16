package uz.academixai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Notification;
import uz.academixai.domain.NotificationType;
import uz.academixai.infrastructure.persistence.NotificationEntity;
import uz.academixai.infrastructure.persistence.NotificationRepository;

/**
 * academix_tz.md §1.15 / §4 {@code sendNotification(userId, type, params)} — deliberately minimal
 * this sprint: persists a {@link Notification} row so the psych-signal notify matrix (task 72) has
 * somewhere real to write, but does NOT deliver via Telegram ({@code sentToTelegram} always stays
 * {@code false}) and there's no inbox/list endpoint yet either — both are real gaps, flagged not
 * silently skipped, deferred to a future Notifications sprint (Telegram bot + notification inbox
 * are separate ROADMAP items, out of scope for "psychological signal monitoring" specifically).
 */
@Service
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final ObjectMapper objectMapper;

  public NotificationService(
      NotificationRepository notificationRepository, ObjectMapper objectMapper) {
    this.notificationRepository = notificationRepository;
    this.objectMapper = objectMapper;
  }

  public void sendNotification(
      UUID userId, NotificationType type, String title, String body, Map<String, String> data) {
    // data column is jsonb — must always be a real JSON document, never a raw string (see
    // NotificationEntity's Javadoc / the lesson_plans.teacher_edited_plan incident in CLAUDE.md).
    String dataJson = toJson(data);
    Notification notification =
        new Notification(
            UUID.randomUUID(),
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
  }

  private String toJson(Map<String, String> data) {
    try {
      return objectMapper.writeValueAsString(data == null ? Map.of() : data);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize notification data", e);
    }
  }
}
