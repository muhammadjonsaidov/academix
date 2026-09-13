package uz.academixai.infrastructure.queue;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.outbox.OutboxService;

/** Writes an exam-submission event to the transactional outbox for durable delivery. */
@Component
public class ExamSubmissionQueueProducer {

  private final OutboxService outbox;

  public ExamSubmissionQueueProducer(OutboxService outbox) {
    this.outbox = outbox;
  }

  public void publish(UUID examSubmissionId, UUID schoolId) {
    outbox.enqueue(
        schoolId,
        "ExamSubmission",
        examSubmissionId,
        "ExamSubmissionAccepted",
        ExamQueueConfig.SUBMISSIONS_QUEUE,
        "submission",
        new SubmissionQueueMessage(examSubmissionId, schoolId));
  }
}
