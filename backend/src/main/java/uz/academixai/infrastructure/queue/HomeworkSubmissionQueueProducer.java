package uz.academixai.infrastructure.queue;

import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Publishes to {@code homework.submissions.queue} — consumed by task 22's OCR+grading listener. */
@Component
public class HomeworkSubmissionQueueProducer {

  private final RabbitTemplate rabbitTemplate;

  public HomeworkSubmissionQueueProducer(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publish(UUID submissionId) {
    rabbitTemplate.convertAndSend(
        HomeworkQueueConfig.SUBMISSIONS_QUEUE, new SubmissionQueueMessage(submissionId));
  }
}
