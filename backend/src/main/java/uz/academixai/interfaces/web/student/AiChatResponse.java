package uz.academixai.interfaces.web.student;

import uz.academixai.domain.ChatBlockReason;
import uz.academixai.intelligence.application.port.in.TutorChat.Result;

/** academix_tz.md §2.3 {@code POST /student/ai-chat} — exact response shape. */
public record AiChatResponse(String response, boolean isBlocked, ChatBlockReason blockReason) {

  public static AiChatResponse from(Result result) {
    return new AiChatResponse(result.response(), result.isBlocked(), result.blockReason());
  }
}
