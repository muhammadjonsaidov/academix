package uz.academixai.intelligence.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.AiChatMessage;
import uz.academixai.domain.SubjectType;
import uz.academixai.infrastructure.persistence.AiChatMessageEntity;
import uz.academixai.infrastructure.persistence.AiChatMessageRepository;
import uz.academixai.intelligence.application.port.out.TutorChatStore;

/** JPA adapter for bounded tutor-chat history. */
@Repository
public class JpaTutorChatStore implements TutorChatStore {

  private final AiChatMessageRepository messages;

  public JpaTutorChatStore(AiChatMessageRepository messages) {
    this.messages = messages;
  }

  @Override
  public AiChatMessage save(AiChatMessage message) {
    return messages.save(AiChatMessageEntity.fromDomain(message)).toDomain();
  }

  @Override
  public List<AiChatMessage> findRecent(UUID studentId, SubjectType subject, int limit) {
    return (subject == null
            ? messages.findByStudentIdOrderByCreatedAtDesc(studentId, Limit.of(limit))
            : messages.findByStudentIdAndSubjectOrderByCreatedAtDesc(
                studentId, subject, Limit.of(limit)))
        .stream().map(AiChatMessageEntity::toDomain).toList();
  }

  @Override
  public long count(UUID studentId, SubjectType subject) {
    return messages.countByStudentIdAndSubject(studentId, subject);
  }

  @Override
  public void deleteOldest(UUID studentId, SubjectType subject) {
    messages
        .findFirstByStudentIdAndSubjectOrderByCreatedAtAsc(studentId, subject)
        .ifPresent(messages::delete);
  }
}
