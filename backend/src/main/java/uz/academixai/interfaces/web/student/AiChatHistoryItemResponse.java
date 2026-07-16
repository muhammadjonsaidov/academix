package uz.academixai.interfaces.web.student;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.AiChatMessage;
import uz.academixai.domain.SubjectType;

/**
 * academix_tz.md §2.3 {@code GET /student/ai-chat/history} — row shape (not exactly spec'd, kept
 * consistent with {@link AiChatResponse}).
 */
public record AiChatHistoryItemResponse(
    UUID id,
    SubjectType subject,
    String message,
    String response,
    boolean isBlocked,
    LocalDateTime createdAt) {

  public static AiChatHistoryItemResponse from(AiChatMessage m) {
    return new AiChatHistoryItemResponse(
        m.id(), m.subject(), m.message(), m.response(), m.isBlocked(), m.createdAt());
  }
}
