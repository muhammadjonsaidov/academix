package uz.academixai.interfaces.web.student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** academix_tz.md §2.3 {@code POST /student/ai-chat} — exact body shape. */
public record AiChatRequest(
    @Size(max = 100) String subject, @NotBlank @Size(max = 4000) String message, Context context) {

  public record Context(UUID assignmentId) {}

  public UUID assignmentId() {
    return context == null ? null : context.assignmentId();
  }
}
