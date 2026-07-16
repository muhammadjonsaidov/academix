package uz.academixai.interfaces.web.student;

import uz.academixai.application.AiChatService.ChatResult;
import uz.academixai.domain.ChatBlockReason;

/** academix_tz.md §2.3 {@code POST /student/ai-chat} — exact response shape. */
public record AiChatResponse(String response, boolean isBlocked, ChatBlockReason blockReason) {

  public static AiChatResponse from(ChatResult result) {
    return new AiChatResponse(result.response(), result.isBlocked(), result.blockReason());
  }
}
