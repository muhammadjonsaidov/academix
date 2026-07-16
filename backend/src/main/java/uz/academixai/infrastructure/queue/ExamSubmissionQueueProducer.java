package uz.academixai.infrastructure.queue;

import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes to {@code exam.submissions.queue}. Same afterCommit-deferral and schoolId-in-message
 * reasoning as {@link HomeworkSubmissionQueueProducer} — see that class's Javadoc.
 */
@Component
public class ExamSubmissionQueueProducer {

  private final RabbitTemplate rabbitTemplate;

  public ExamSubmissionQueueProducer(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publish(UUID examSubmissionId, UUID schoolId) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              doPublish(examSubmissionId, schoolId);
            }
          });
    } else {
      doPublish(examSubmissionId, schoolId);
    }
  }

  private void doPublish(UUID examSubmissionId, UUID schoolId) {
    rabbitTemplate.convertAndSend(
        ExamQueueConfig.SUBMISSIONS_QUEUE, new SubmissionQueueMessage(examSubmissionId, schoolId));
  }
}
