package uz.academixai.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.academixai.domain.SubjectType;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessageEntity, UUID> {

  long countByStudentIdAndSubject(UUID studentId, SubjectType subject);

  Optional<AiChatMessageEntity> findFirstByStudentIdAndSubjectOrderByCreatedAtAsc(
      UUID studentId, SubjectType subject);

  List<AiChatMessageEntity> findByStudentIdOrderByCreatedAtDesc(UUID studentId, Limit limit);

  List<AiChatMessageEntity> findByStudentIdAndSubjectOrderByCreatedAtDesc(
      UUID studentId, SubjectType subject, Limit limit);
}
