package uz.academixai.learning.infrastructure.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uz.academixai.domain.ExamSubmission;
import uz.academixai.infrastructure.persistence.ExamSubmissionEntity;
import uz.academixai.infrastructure.persistence.ExamSubmissionRepository;
import uz.academixai.infrastructure.queue.ExamSubmissionQueueProducer;
import uz.academixai.learning.application.port.out.ExamSubmissionAcceptor;
import uz.academixai.shared.tenancy.TenantScope;

/**
 * Persists one scanned paper and its outbox event atomically in an independent batch transaction.
 *
 * <p>Runs inside that transaction's tenant scope: {@code exam_submissions} is RLS-enabled and a
 * batch of papers may arrive while the caller's scope is a different one, so each paper re-declares
 * its own school before writing.
 */
@Component
public class OutboxExamSubmissionAcceptor implements ExamSubmissionAcceptor {

  private final ExamSubmissionRepository submissions;
  private final ExamSubmissionQueueProducer publisher;
  private final TenantScope tenantScope;

  public OutboxExamSubmissionAcceptor(
      ExamSubmissionRepository submissions,
      ExamSubmissionQueueProducer publisher,
      TenantScope tenantScope) {
    this.submissions = submissions;
    this.publisher = publisher;
    this.tenantScope = tenantScope;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void accept(ExamSubmission submission) {
    tenantScope.runAsTenant(
        submission.schoolId(),
        null,
        () -> {
          ExamSubmission saved =
              submissions.save(ExamSubmissionEntity.fromDomain(submission)).toDomain();
          publisher.publish(saved.id(), saved.schoolId());
        });
  }
}
