package uz.academixai.telegrambot;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Updates {@code notifications.sent_to_telegram} once a send actually succeeds — the row itself is
 * always created by backend (inbox is the source of truth, per NotificationService's own Javadoc);
 * this service only ever flips one boolean on an existing row after real delivery. Not RLS-enabled,
 * plain update by id.
 */
@Repository
public class NotificationStatusRepository {

  private final JdbcTemplate jdbc;

  public NotificationStatusRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void markSentToTelegram(UUID notificationId) {
    jdbc.update("UPDATE notifications SET sent_to_telegram = true WHERE id = ?", notificationId);
  }
}
