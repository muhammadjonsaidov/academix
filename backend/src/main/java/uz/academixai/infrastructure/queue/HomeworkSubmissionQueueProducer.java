package uz.academixai.infrastructure.queue;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.outbox.OutboxService;

/**
 * Publishes to {@code homework.submissions.queue} — consumed by task 22's OCR+grading listener.
 *
 * <p>The message is written to the transactional outbox with the submission. This closes the old
 * after-commit failure window where a database commit could succeed but the process could die
 * before RabbitMQ received the message.
 *
 * <p><b>{@code schoolId} rides along in the message on purpose</b> — the listener has no HTTP
 * request to derive it from (unlike every other RLS-touching code path), so it can't run its own
 * {@code SET LOCAL app.current_school_id} without already knowing it. Confirmed necessary by a real
 * {@code invalid input syntax for type uuid: ""} failure the first time the consumer tried to read
 * the RLS-enabled {@code homework_submissions} table with no session variable set at all.
 */
@Component
public class HomeworkSubmissionQueueProducer {

  private final OutboxService outbox;

  public HomeworkSubmissionQueueProducer(OutboxService outbox) {
    this.outbox = outbox;
  }

  public void publish(UUID submissionId, UUID schoolId) {
    outbox.enqueue(
        schoolId,
        "HomeworkSubmission",
        submissionId,
        "HomeworkSubmissionAccepted",
        HomeworkQueueConfig.SUBMISSIONS_QUEUE,
        "submission",
        new SubmissionQueueMessage(submissionId, schoolId));
  }
}
