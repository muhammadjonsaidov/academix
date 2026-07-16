package uz.academixai.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamAIFeedbackRepository extends JpaRepository<ExamAIFeedbackEntity, UUID> {

  Optional<ExamAIFeedbackEntity> findByExamSubmissionId(UUID examSubmissionId);
}
