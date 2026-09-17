package uz.academixai.learning.infrastructure.messaging;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.queue.HomeworkSubmissionQueueProducer;
import uz.academixai.learning.application.port.out.HomeworkSubmissionPublisher;

/** Adapter that records the submission-accepted event in the transactional outbox. */
@Component
public class OutboxHomeworkSubmissionPublisher implements HomeworkSubmissionPublisher {

  private final HomeworkSubmissionQueueProducer producer;

  public OutboxHomeworkSubmissionPublisher(HomeworkSubmissionQueueProducer producer) {
    this.producer = producer;
  }

  @Override
  public void publish(UUID submissionId, UUID schoolId) {
    producer.publish(submissionId, schoolId);
  }
}
