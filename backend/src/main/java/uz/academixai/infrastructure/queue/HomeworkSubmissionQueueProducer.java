package uz.academixai.infrastructure.queue;

import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes to {@code homework.submissions.queue} — consumed by task 22's OCR+grading listener.
 *
 * <p><b>Publishes only after the current transaction commits, if one is active.</b> {@code
 * RlsTransactionFilter} wraps the whole HTTP request (including the controller that calls this) in
 * one transaction — publishing synchronously would let a fast consumer try {@code
 * submissionRepo.findById(submissionId)} before that transaction's INSERT is actually durable, a
 * real race (the spec's own consumer pseudocode does exactly that lookup as its first line). {@code
 * TransactionSynchronizationManager.registerSynchronization(afterCommit)} defers the publish until
 * the row genuinely exists. Falls back to publishing immediately when no transaction is active
 * (e.g. a future non-request caller).
 */
@Component
public class HomeworkSubmissionQueueProducer {

  private final RabbitTemplate rabbitTemplate;

  public HomeworkSubmissionQueueProducer(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publish(UUID submissionId) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              doPublish(submissionId);
            }
          });
    } else {
      doPublish(submissionId);
    }
  }

  private void doPublish(UUID submissionId) {
    rabbitTemplate.convertAndSend(
        HomeworkQueueConfig.SUBMISSIONS_QUEUE, new SubmissionQueueMessage(submissionId));
  }
}
