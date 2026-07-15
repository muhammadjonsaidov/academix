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
 *
 * <p><b>{@code schoolId} rides along in the message on purpose</b> — the listener has no HTTP
 * request to derive it from (unlike every other RLS-touching code path), so it can't run its own
 * {@code SET LOCAL app.current_school_id} without already knowing it. Confirmed necessary by a real
 * {@code invalid input syntax for type uuid: ""} failure the first time the consumer tried to read
 * the RLS-enabled {@code homework_submissions} table with no session variable set at all.
 */
@Component
public class HomeworkSubmissionQueueProducer {

  private final RabbitTemplate rabbitTemplate;

  public HomeworkSubmissionQueueProducer(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publish(UUID submissionId, UUID schoolId) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              doPublish(submissionId, schoolId);
            }
          });
    } else {
      doPublish(submissionId, schoolId);
    }
  }

  private void doPublish(UUID submissionId, UUID schoolId) {
    rabbitTemplate.convertAndSend(
        HomeworkQueueConfig.SUBMISSIONS_QUEUE, new SubmissionQueueMessage(submissionId, schoolId));
  }
}
