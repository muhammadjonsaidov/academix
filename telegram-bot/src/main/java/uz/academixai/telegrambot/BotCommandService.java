package uz.academixai.telegrambot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Student-facing bot commands over the shared database. Everything here is read-only.
 *
 * <p>RLS: this service connects as the restricted {@code academix_app} role, and {@code
 * homework_submissions}/{@code homework_assignments} are RLS-enabled — so any query touching them
 * runs inside an explicit transaction that first executes {@code SET LOCAL app.current_school_id},
 * the same manual pattern the backend's own RabbitMQ consumer and nightly psychology job use (see
 * CLAUDE.md). The school id comes from {@code student_profiles} (not RLS-enabled), and is always a
 * UUID read from the database — never raw user input — which is the documented precondition for
 * inlining it into the {@code SET LOCAL} statement ({@code SET} can't take bind parameters).
 */
@Service
public class BotCommandService {

  private static final Logger log = LoggerFactory.getLogger(BotCommandService.class);

  private final JdbcTemplate jdbc;
  private final TransactionTemplate transactionTemplate;

  public BotCommandService(JdbcTemplate jdbc, TransactionTemplate transactionTemplate) {
    this.jdbc = jdbc;
    this.transactionTemplate = transactionTemplate;
  }

  public static final String HELP_TEXT =
      """
      AcademiX AI bot buyruqlari:

      /baholar — so'nggi 5 ta bahoyingiz
      /xp — XP, daraja va streak holatingiz
      /vazifalar — muddati kelmagan uy vazifalari
      /help — shu ro'yxat

      Bildirishnomalar avtomatik keladi — hech narsa qilish shart emas.
      """;

  public String handle(long chatId, String command) {
    Optional<UUID> userId = findUserByChatId(chatId);
    if (userId.isEmpty()) {
      return "Hisobingiz hali ulanmagan. Ilovadagi \"Telegram'ni ulash\" tugmasi orqali havola oling.";
    }
    return switch (command) {
      case "/baholar" -> grades(userId.get());
      case "/xp" -> xp(userId.get());
      case "/vazifalar" -> upcomingHomework(userId.get());
      default -> HELP_TEXT;
    };
  }

  private Optional<UUID> findUserByChatId(long chatId) {
    return jdbc
        .query(
            "SELECT user_id FROM telegram_connections WHERE telegram_chat_id = ? AND is_active = true",
            (rs, i) -> UUID.fromString(rs.getString("user_id")),
            chatId)
        .stream()
        .findFirst();
  }

  private record StudentContext(UUID schoolId, UUID classId) {}

  private Optional<StudentContext> studentContext(UUID userId) {
    return jdbc
        .query(
            "SELECT school_id, class_id FROM student_profiles WHERE user_id = ?",
            (rs, i) ->
                new StudentContext(
                    UUID.fromString(rs.getString("school_id")),
                    rs.getString("class_id") == null
                        ? null
                        : UUID.fromString(rs.getString("class_id"))),
            userId)
        .stream()
        .findFirst();
  }

  private String grades(UUID userId) {
    Optional<StudentContext> context = studentContext(userId);
    if (context.isEmpty()) {
      return "Bu buyruq faqat o'quvchilar uchun.";
    }
    try {
      List<String> lines =
          transactionTemplate.execute(
              status -> {
                setSchoolContext(context.get().schoolId());
                return jdbc.query(
                    """
                    SELECT ha.title, g.score, g.five_point_grade, g.graded_at::date AS graded_on
                    FROM grades g
                    JOIN homework_submissions hs ON hs.id = g.submission_id
                    JOIN homework_assignments ha ON ha.id = hs.assignment_id
                    WHERE hs.student_id = ?
                    ORDER BY g.graded_at DESC
                    LIMIT 5
                    """,
                    (rs, i) ->
                        "%s — %d%% (baho: %d) · %s"
                            .formatted(
                                rs.getString("title"),
                                rs.getInt("score"),
                                rs.getInt("five_point_grade"),
                                rs.getDate("graded_on")),
                    userId);
              });
      if (lines == null || lines.isEmpty()) {
        return "Hali baholangan ishlaringiz yo'q.";
      }
      return "So'nggi baholaringiz:\n\n" + String.join("\n", lines);
    } catch (Exception e) {
      log.warn("Failed to load grades for user {}", userId, e);
      return "Baholarni olishda xatolik. Birozdan so'ng qayta urinib ko'ring.";
    }
  }

  private String xp(UUID userId) {
    return jdbc
        .query(
            "SELECT total_xp, current_streak, max_streak FROM student_profiles WHERE user_id = ?",
            (rs, i) ->
                """
                XP holatingiz:

                Jami XP: %d
                Joriy streak: %d kun
                Eng uzun streak: %d kun
                """
                    .formatted(
                        rs.getInt("total_xp"), rs.getInt("current_streak"), rs.getInt("max_streak")),
            userId)
        .stream()
        .findFirst()
        .orElse("Bu buyruq faqat o'quvchilar uchun.");
  }

  private String upcomingHomework(UUID userId) {
    Optional<StudentContext> context = studentContext(userId);
    if (context.isEmpty() || context.get().classId() == null) {
      return "Bu buyruq faqat sinfga biriktirilgan o'quvchilar uchun.";
    }
    try {
      List<String> lines =
          transactionTemplate.execute(
              status -> {
                setSchoolContext(context.get().schoolId());
                return jdbc.query(
                    """
                    SELECT ha.title, ha.deadline_at
                    FROM homework_assignments ha
                    WHERE ha.class_id = ?
                      AND ha.is_active = true
                      AND ha.tasks_published = true
                      AND ha.deadline_at > CURRENT_TIMESTAMP
                      AND NOT EXISTS (
                        SELECT 1 FROM homework_submissions hs
                        WHERE hs.assignment_id = ha.id AND hs.student_id = ?
                      )
                    ORDER BY ha.deadline_at ASC
                    LIMIT 10
                    """,
                    (rs, i) ->
                        "%s — muddat: %s"
                            .formatted(
                                rs.getString("title"),
                                rs.getTimestamp("deadline_at").toLocalDateTime().toLocalDate()),
                    context.get().classId(),
                    userId);
              });
      if (lines == null || lines.isEmpty()) {
        return "Topshirilmagan vazifangiz yo'q — barakalla!";
      }
      return "Muddati kelmagan vazifalar:\n\n" + String.join("\n", lines);
    } catch (Exception e) {
      log.warn("Failed to load homework for user {}", userId, e);
      return "Vazifalarni olishda xatolik. Birozdan so'ng qayta urinib ko'ring.";
    }
  }

  // Same inlined-literal SET LOCAL as backend's RlsTransactionFilter — the value is always a
  // UUID object read from our own database, never raw input (SET takes no bind parameters).
  private void setSchoolContext(UUID schoolId) {
    jdbc.execute("SET LOCAL app.current_school_id = '" + schoolId + "'");
  }
}
