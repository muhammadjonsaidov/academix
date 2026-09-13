package uz.academixai.learning.application.port.out;

import java.util.UUID;

/** Durable integration-event boundary for an accepted submission. */
public interface HomeworkSubmissionPublisher {

  void publish(UUID submissionId, UUID schoolId);
}
