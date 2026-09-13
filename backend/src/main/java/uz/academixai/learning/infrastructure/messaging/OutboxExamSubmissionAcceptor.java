package uz.academixai.learning.infrastructure.messaging;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uz.academixai.domain.ExamSubmission;
import uz.academixai.infrastructure.persistence.ExamSubmissionEntity;
import uz.academixai.infrastructure.persistence.ExamSubmissionRepository;
import uz.academixai.infrastructure.queue.ExamSubmissionQueueProducer;
import uz.academixai.learning.application.port.out.ExamSubmissionAcceptor;

/**
 * Persists one scanned paper and its outbox event atomically in an independent batch transaction.
 */
@Component
public class OutboxExamSubmissionAcceptor implements ExamSubmissionAcceptor {

  private final ExamSubmissionRepository submissions;
  private final ExamSubmissionQueueProducer publisher;
  private final EntityManager entityManager;

  public OutboxExamSubmissionAcceptor(
      ExamSubmissionRepository submissions,
      ExamSubmissionQueueProducer publisher,
      EntityManager entityManager) {
    this.submissions = submissions;
    this.publisher = publisher;
    this.entityManager = entityManager;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void accept(ExamSubmission submission) {
    entityManager
        .createNativeQuery("SET LOCAL app.current_school_id = '" + submission.schoolId() + "'")
        .executeUpdate();
    ExamSubmission saved = submissions.save(ExamSubmissionEntity.fromDomain(submission)).toDomain();
    publisher.publish(saved.id(), saved.schoolId());
  }
}
