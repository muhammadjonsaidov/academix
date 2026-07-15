package uz.academixai.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIFeedbackRepository extends JpaRepository<AIFeedbackEntity, UUID> {

  Optional<AIFeedbackEntity> findBySubmissionId(UUID submissionId);
}
