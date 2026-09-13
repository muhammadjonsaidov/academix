package uz.academixai.intelligence.application.port.out;

import java.util.List;
import java.util.UUID;
import uz.academixai.domain.AiChatMessage;
import uz.academixai.domain.SubjectType;

/** Bounded chat-history persistence boundary. */
public interface TutorChatStore {

  AiChatMessage save(AiChatMessage message);

  List<AiChatMessage> findRecent(UUID studentId, SubjectType subject, int limit);

  long count(UUID studentId, SubjectType subject);

  void deleteOldest(UUID studentId, SubjectType subject);
}
