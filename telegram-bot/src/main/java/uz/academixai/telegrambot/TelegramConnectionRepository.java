package uz.academixai.telegrambot;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Plain JdbcTemplate against the shared {@code telegram_connections} table — this table is NOT
 * RLS-enabled (confirmed against CLAUDE.md's 9-table RLS list), so no {@code SET LOCAL
 * app.current_school_id} dance is needed here, unlike backend's own RLS-touching background jobs
 * (the nightly psychology job, the RabbitMQ homework consumer). Full JPA felt disproportionate for
 * a 6-column table touched by exactly two queries.
 */
@Repository
public class TelegramConnectionRepository {

  private final JdbcTemplate jdbc;

  public TelegramConnectionRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * {@code user_id} carries a UNIQUE constraint — upsert on conflict, matching backend's own
   * upsertConnection semantics (a user reconnecting replaces their old chat binding).
   */
  public void upsertConnection(UUID userId, long chatId, String username) {
    jdbc.update(
        """
        INSERT INTO telegram_connections (id, user_id, telegram_chat_id, telegram_username, is_active, connected_at)
        VALUES (uuid_generate_v4(), ?, ?, ?, true, CURRENT_TIMESTAMP)
        ON CONFLICT (user_id) DO UPDATE
          SET telegram_chat_id = EXCLUDED.telegram_chat_id,
              telegram_username = EXCLUDED.telegram_username,
              is_active = true
        """,
        userId,
        chatId,
        username);
  }

  public Optional<Long> findActiveChatId(UUID userId) {
    return jdbc
        .query(
            "SELECT telegram_chat_id FROM telegram_connections WHERE user_id = ? AND is_active = true",
            (rs, rowNum) -> rs.getLong("telegram_chat_id"),
            userId)
        .stream()
        .findFirst();
  }
}
