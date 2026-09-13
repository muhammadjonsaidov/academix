package uz.academixai.intelligence.application.port.in;

import java.util.List;
import java.util.UUID;
import uz.academixai.domain.AiChatMessage;
import uz.academixai.domain.ChatBlockReason;

/** Published student-facing AI tutor chat capability. */
public interface TutorChat {

  record Result(String response, boolean isBlocked, ChatBlockReason blockReason) {}

  Result chat(UUID schoolId, UUID studentId, String subject, String message, UUID assignmentId);

  List<AiChatMessage> history(UUID studentId, String subject, int limit);
}
