package uz.academixai.infrastructure.queue;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import uz.academixai.application.SyllabusIngestionService;

/**
 * The HTTP upload request completes quickly; this worker makes the file usable by AI afterwards.
 */
@Component
public class SyllabusIngestionListener {

  private final SyllabusIngestionService ingestion;

  public SyllabusIngestionListener(SyllabusIngestionService ingestion) {
    this.ingestion = ingestion;
  }

  @RabbitListener(queues = SyllabusIngestionQueueConfig.INGESTION_QUEUE)
  public void onIngestion(SyllabusIngestionMessage message) {
    ingestion.ingest(message.syllabusId());
  }
}
