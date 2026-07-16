package uz.academixai.interfaces.web.student;

import java.util.UUID;

/** academix_tz.md §2.3 {@code POST /student/ai-chat} — exact body shape. */
public record AiChatRequest(String subject, String message, Context context) {

  public record Context(UUID assignmentId) {}

  public UUID assignmentId() {
    return context == null ? null : context.assignmentId();
  }
}
