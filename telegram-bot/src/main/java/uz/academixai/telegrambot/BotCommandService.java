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
 * Role-aware bot commands over the shared database. Everything here is read-only. The chat is
 * resolved to a user, the user's role decides both the visible command set (/help) and which
 * handlers are reachable — a TEACHER chat never sees or executes student commands and vice versa.
 *
 * <p>RLS: this service connects as the restricted {@code academix_app} role; queries touching
 * RLS-enabled tables ({@code homework_submissions}/{@code homework_assignments}/{@code
 * class_subject_teachers}) run inside an explicit transaction that first executes {@code SET LOCAL
 * app.current_school_id} — the same manual pattern the backend's own background consumers use (see
 * CLAUDE.md). The school id is always a UUID read from our own database — never raw user input —
 * which is the documented precondition for inlining it into the {@code SET LOCAL} statement ({@code
 * SET} can't take bind parameters).
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

  private record BotUser(UUID id, String role, UUID schoolId) {}

  public String handle(long chatId, String command) {
    Optional<BotUser> user = findUserByChatId(chatId);
    if (user.isEmpty()) {
      return "Hisobingiz hali ulanmagan. Ilovadagi \"Telegram'ni ulash\" tugmasi orqali havola oling.";
    }
    return dispatch(user.get(), command);
  }

  /** Role-scoped welcome shown right after a successful /start link. */
  public String welcomeText(UUID userId) {
    return loadUser(userId).map(this::helpText).orElse("Ulanish muvaffaqiyatli!");
  }

  private String dispatch(BotUser user, String command) {
    return switch (user.role()) {
      case "STUDENT" ->
          switch (command) {
            case "/baholar" -> studentGrades(user);
            case "/xp" -> studentXp(user);
            case "/vazifalar" -> studentHomework(user);
            default -> helpText(user);
          };
      case "TEACHER" ->
          switch (command) {
            case "/sinflarim" -> teacherClasses(user);
            case "/tekshirish" -> teacherPendingGrading(user);
            default -> helpText(user);
          };
      case "PARENT" ->
          switch (command) {
            case "/farzandlarim" -> parentChildren(user);
            default -> helpText(user);
          };
      case "PSYCHOLOGIST" ->
          switch (command) {
            case "/signallar" -> psychologistSignals(user);
            default -> helpText(user);
          };
      case "ADMIN" ->
          switch (command) {
            case "/statistika" -> adminStats(user);
            default -> helpText(user);
          };
      default -> helpText(user);
    };
  }

  private String helpText(BotUser user) {
    String commands =
        switch (user.role()) {
          case "STUDENT" ->
              """
              /baholar — so'nggi 5 ta bahoyingiz
              /xp — XP va streak holatingiz
              /vazifalar — muddati kelmagan uy vazifalari
              """;
          case "TEACHER" ->
              """
              /sinflarim — sinflaringiz va o'quvchilar soni
              /tekshirish — baholashni kutayotgan ishlar
              """;
          case "PARENT" -> "/farzandlarim — farzandlaringizning so'nggi baholari\n";
          case "PSYCHOLOGIST" -> "/signallar — ochiq psixologik signallar\n";
          case "ADMIN" -> "/statistika — maktab bo'yicha umumiy raqamlar\n";
          default -> "";
        };
    return "AcademiX AI bot buyruqlari:\n\n"
        + commands
        + "/help — shu ro'yxat\n\nBildirishnomalar avtomatik keladi.";
  }

  // ---- lookups -------------------------------------------------------------

  private Optional<BotUser> findUserByChatId(long chatId) {
    return jdbc
        .query(
            "SELECT user_id FROM telegram_connections WHERE telegram_chat_id = ? AND is_active = true",
            (rs, i) -> UUID.fromString(rs.getString("user_id")),
            chatId)
        .stream()
        .findFirst()
        .flatMap(this::loadUser);
  }

  private Optional<BotUser> loadUser(UUID userId) {
    return jdbc
        .query(
            // school resolution mirrors backend SchoolContextResolver: TEACHER/PSYCHOLOGIST via
            // users.school_id, STUDENT via student_profiles, ADMIN via schools.admin_id, PARENT
            // via the first linked child's profile.
            """
            SELECT u.id, u.role,
              COALESCE(
                u.school_id,
                (SELECT sp.school_id FROM student_profiles sp WHERE sp.user_id = u.id),
                (SELECT s.id FROM schools s WHERE s.admin_id = u.id),
                (SELECT sp.school_id
                   FROM parent_student_links l
                   JOIN student_profiles sp ON sp.user_id = l.student_user_id
                  WHERE l.parent_user_id = u.id AND l.is_active = true
                  LIMIT 1)
              ) AS school_id
            FROM users u WHERE u.id = ?
            """,
            (rs, i) ->
                new BotUser(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("role"),
                    rs.getString("school_id") == null
                        ? null
                        : UUID.fromString(rs.getString("school_id"))),
            userId)
        .stream()
        .findFirst();
  }

  // ---- student -------------------------------------------------------------

  private String studentGrades(BotUser user) {
    if (user.schoolId() == null) return "Maktab ma'lumoti topilmadi.";
    try {
      List<String> lines =
          inSchoolContext(
              user.schoolId(),
              () ->
                  jdbc.query(
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
                      user.id()));
      return lines.isEmpty()
          ? "Hali baholangan ishlaringiz yo'q."
          : "So'nggi baholaringiz:\n\n" + String.join("\n", lines);
    } catch (Exception e) {
      log.warn("grades failed for {}", user.id(), e);
      return "Baholarni olishda xatolik. Birozdan so'ng qayta urinib ko'ring.";
    }
  }

  private String studentXp(BotUser user) {
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
                        rs.getInt("total_xp"),
                        rs.getInt("current_streak"),
                        rs.getInt("max_streak")),
            user.id())
        .stream()
        .findFirst()
        .orElse("O'quvchi profili topilmadi.");
  }

  private String studentHomework(BotUser user) {
    UUID classId =
        jdbc
            .query(
                "SELECT class_id FROM student_profiles WHERE user_id = ?",
                (rs, i) ->
                    rs.getString("class_id") == null
                        ? null
                        : UUID.fromString(rs.getString("class_id")),
                user.id())
            .stream()
            .findFirst()
            .orElse(null);
    if (user.schoolId() == null || classId == null) {
      return "Sinf ma'lumoti topilmadi.";
    }
    try {
      List<String> lines =
          inSchoolContext(
              user.schoolId(),
              () ->
                  jdbc.query(
                      """
                      SELECT ha.title, ha.deadline_at
                      FROM homework_assignments ha
                      WHERE ha.class_id = ?
                        AND ha.is_active = true AND ha.tasks_published = true
                        AND ha.deadline_at > CURRENT_TIMESTAMP
                        AND NOT EXISTS (SELECT 1 FROM homework_submissions hs
                                        WHERE hs.assignment_id = ha.id AND hs.student_id = ?)
                      ORDER BY ha.deadline_at ASC LIMIT 10
                      """,
                      (rs, i) ->
                          "%s — muddat: %s"
                              .formatted(
                                  rs.getString("title"),
                                  rs.getTimestamp("deadline_at").toLocalDateTime().toLocalDate()),
                      classId,
                      user.id()));
      return lines.isEmpty()
          ? "Topshirilmagan vazifangiz yo'q — barakalla!"
          : "Muddati kelmagan vazifalar:\n\n" + String.join("\n", lines);
    } catch (Exception e) {
      log.warn("homework failed for {}", user.id(), e);
      return "Vazifalarni olishda xatolik. Birozdan so'ng qayta urinib ko'ring.";
    }
  }

  // ---- teacher -------------------------------------------------------------

  private String teacherClasses(BotUser user) {
    if (user.schoolId() == null) return "Maktab ma'lumoti topilmadi.";
    try {
      List<String> lines =
          inSchoolContext(
              user.schoolId(),
              () ->
                  jdbc.query(
                      """
                      SELECT DISTINCT sc.full_name, sc.student_count
                      FROM class_subject_teachers cst
                      JOIN school_classes sc ON sc.id = cst.class_id
                      WHERE cst.teacher_id = ?
                      ORDER BY sc.full_name
                      """,
                      (rs, i) ->
                          "%s — %d o'quvchi"
                              .formatted(rs.getString("full_name"), rs.getInt("student_count")),
                      user.id()));
      return lines.isEmpty()
          ? "Sizga hali sinf biriktirilmagan."
          : "Sinflaringiz:\n\n" + String.join("\n", lines);
    } catch (Exception e) {
      log.warn("classes failed for {}", user.id(), e);
      return "Sinflarni olishda xatolik. Birozdan so'ng qayta urinib ko'ring.";
    }
  }

  private String teacherPendingGrading(BotUser user) {
    if (user.schoolId() == null) return "Maktab ma'lumoti topilmadi.";
    try {
      Integer pending =
          inSchoolContext(
              user.schoolId(),
              () ->
                  jdbc.queryForObject(
                      """
                      SELECT COUNT(*) FROM homework_submissions hs
                      JOIN homework_assignments ha ON ha.id = hs.assignment_id
                      WHERE ha.teacher_id = ? AND hs.status IN ('AI_DONE','AI_SKIPPED')
                      """,
                      Integer.class,
                      user.id()));
      int count = pending == null ? 0 : pending;
      return count == 0
          ? "Baholashni kutayotgan ish yo'q — hammasi tekshirilgan!"
          : "Baholashni kutayotgan ishlar: %d ta.\nIlovada \"Topshirilgan ishlar\" bo'limini oching."
              .formatted(count);
    } catch (Exception e) {
      log.warn("pending grading failed for {}", user.id(), e);
      return "Ma'lumotni olishda xatolik. Birozdan so'ng qayta urinib ko'ring.";
    }
  }

  // ---- parent --------------------------------------------------------------

  private String parentChildren(BotUser user) {
    if (user.schoolId() == null) return "Farzand ma'lumoti topilmadi.";
    try {
      List<String> lines =
          inSchoolContext(
              user.schoolId(),
              () ->
                  jdbc.query(
                      """
                      SELECT u.first_name, u.last_name,
                             (SELECT 'so''nggi baho: ' || g.score || '%% (' || g.five_point_grade || ')'
                                FROM grades g
                                JOIN homework_submissions hs ON hs.id = g.submission_id
                               WHERE hs.student_id = u.id
                               ORDER BY g.graded_at DESC LIMIT 1) AS last_grade
                      FROM parent_student_links l
                      JOIN users u ON u.id = l.student_user_id
                      WHERE l.parent_user_id = ? AND l.is_active = true
                      ORDER BY u.first_name
                      """,
                      (rs, i) -> {
                        String grade = rs.getString("last_grade");
                        return "%s %s — %s"
                            .formatted(
                                rs.getString("first_name"),
                                rs.getString("last_name"),
                                grade == null ? "hali baho yo'q" : grade);
                      },
                      user.id()));
      return lines.isEmpty()
          ? "Sizga hali farzand biriktirilmagan."
          : "Farzandlaringiz:\n\n" + String.join("\n", lines);
    } catch (Exception e) {
      log.warn("children failed for {}", user.id(), e);
      return "Ma'lumotni olishda xatolik. Birozdan so'ng qayta urinib ko'ring.";
    }
  }

  // ---- psychologist / admin ----------------------------------------------

  private String psychologistSignals(BotUser user) {
    try {
      return jdbc
          .query(
              """
              SELECT
                COUNT(*) FILTER (WHERE severity='CRITICAL' AND is_resolved=false) AS critical,
                COUNT(*) FILTER (WHERE severity='HIGH' AND is_resolved=false) AS high,
                COUNT(*) FILTER (WHERE severity='MEDIUM' AND is_resolved=false) AS medium
              FROM psychological_signals
              """,
              (rs, i) ->
                  """
                  Ochiq psixologik signallar:

                  Kritik: %d
                  Yuqori: %d
                  O'rta: %d

                  Batafsil: ilovadagi "Signallar" bo'limi.
                  """
                      .formatted(rs.getInt("critical"), rs.getInt("high"), rs.getInt("medium")),
              new Object[] {})
          .stream()
          .findFirst()
          .orElse("Signallar topilmadi.");
    } catch (Exception e) {
      log.warn("signals failed for {}", user.id(), e);
      return "Ma'lumotni olishda xatolik. Birozdan so'ng qayta urinib ko'ring.";
    }
  }

  private String adminStats(BotUser user) {
    if (user.schoolId() == null) return "Maktab ma'lumoti topilmadi.";
    try {
      return jdbc
          .query(
              """
              SELECT
                (SELECT COUNT(*) FROM student_profiles WHERE school_id = ?) AS students,
                (SELECT COUNT(*) FROM users WHERE school_id = ? AND role = 'TEACHER') AS teachers,
                (SELECT COUNT(*) FROM school_classes WHERE school_id = ? AND is_active = true) AS classes
              """,
              (rs, i) ->
                  """
                  Maktab statistikasi:

                  O'quvchilar: %d
                  O'qituvchilar: %d
                  Sinflar: %d
                  """
                      .formatted(
                          rs.getInt("students"), rs.getInt("teachers"), rs.getInt("classes")),
              user.schoolId(),
              user.schoolId(),
              user.schoolId())
          .stream()
          .findFirst()
          .orElse("Statistika topilmadi.");
    } catch (Exception e) {
      log.warn("stats failed for {}", user.id(), e);
      return "Ma'lumotni olishda xatolik. Birozdan so'ng qayta urinib ko'ring.";
    }
  }

  // ---- infrastructure ------------------------------------------------------

  private <T> T inSchoolContext(UUID schoolId, java.util.function.Supplier<T> query) {
    return transactionTemplate.execute(
        status -> {
          // Inlined-literal SET LOCAL, same as backend's RlsTransactionFilter — the value is a
          // UUID from our own database, never raw input (SET takes no bind parameters).
          jdbc.execute("SET LOCAL app.current_school_id = '" + schoolId + "'");
          return query.get();
        });
  }
}
