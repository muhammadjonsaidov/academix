package uz.academixai.learning.infrastructure.messaging;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.outbox.OutboxService;
import uz.academixai.infrastructure.queue.SyllabusIngestionMessage;
import uz.academixai.infrastructure.queue.SyllabusIngestionQueueConfig;
import uz.academixai.learning.application.port.out.SyllabusIngestionPublisher;

/** Publishes the ingestion request through the transactional outbox, same as submissions do. */
@Component
public class OutboxSyllabusIngestionPublisher implements SyllabusIngestionPublisher {

  private final OutboxService outbox;

  public OutboxSyllabusIngestionPublisher(OutboxService outbox) {
    this.outbox = outbox;
  }

  @Override
  public void requestIngestion(UUID schoolId, UUID syllabusId) {
    outbox.enqueue(
        schoolId,
        "TeacherSyllabus",
        syllabusId,
        "SyllabusUploaded",
        SyllabusIngestionQueueConfig.INGESTION_QUEUE,
        "syllabusIngestion",
        new SyllabusIngestionMessage(schoolId, syllabusId));
  }
}
