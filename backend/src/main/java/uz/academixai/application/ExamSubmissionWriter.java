package uz.academixai.application;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uz.academixai.domain.ExamSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.infrastructure.persistence.ExamSubmissionEntity;
import uz.academixai.infrastructure.persistence.ExamSubmissionRepository;
import uz.academixai.infrastructure.queue.ExamSubmissionQueueProducer;

/**
 * Separate bean (not a private method on {@link ExamSubmissionService}) purely so
 * {@code @Transactional(REQUIRES_NEW)} actually applies — Spring's proxy-based AOP doesn't
 * intercept self-invocation ({@code this.method()} calls from within the same class silently skip
 * the transactional advice entirely, a well-known Spring pitfall). {@link
 * ExamSubmissionService#bulkUpload} calls this through the injected bean reference instead, so each
 * submission in a batch genuinely gets its own independently-committed transaction — see that
 * method's Javadoc for why REQUIRES_NEW is needed here at all.
 */
@Service
public class ExamSubmissionWriter {

  private final ExamSubmissionRepository submissionRepository;
  private final ExamSubmissionQueueProducer queueProducer;
  private final EntityManager entityManager;

  public ExamSubmissionWriter(
      ExamSubmissionRepository submissionRepository,
      ExamSubmissionQueueProducer queueProducer,
      EntityManager entityManager) {
    this.submissionRepository = submissionRepository;
    this.queueProducer = queueProducer;
    this.entityManager = entityManager;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createSubmission(UUID schoolId, UUID examId, UUID studentId, String imageUrl) {
    entityManager
        .createNativeQuery("SET LOCAL app.current_school_id = '" + schoolId + "'")
        .executeUpdate();
    ExamSubmission submission =
        new ExamSubmission(
            UUID.randomUUID(),
            schoolId,
            examId,
            studentId,
            imageUrl,
            SubmissionStatus.AI_PROCESSING,
            false,
            LocalDateTime.now());
    ExamSubmission saved =
        submissionRepository.save(ExamSubmissionEntity.fromDomain(submission)).toDomain();
    queueProducer.publish(saved.id(), schoolId);
  }
}
