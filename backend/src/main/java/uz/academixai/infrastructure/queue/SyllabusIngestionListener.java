package uz.academixai.infrastructure.queue;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import uz.academixai.learning.application.SyllabusIngestionService;
import uz.academixai.shared.tenancy.TenantScope;

/**
 * The HTTP upload request completes quickly; this worker makes the file usable by AI afterwards.
 *
 * <p>Runs in the uploading teacher's tenant scope: the message carries the school because there is
 * no request here to derive one from, and the tables this worker writes are RLS candidates.
 */
@Component
public class SyllabusIngestionListener {

  private final SyllabusIngestionService ingestion;
  private final TenantScope tenantScope;

  public SyllabusIngestionListener(SyllabusIngestionService ingestion, TenantScope tenantScope) {
    this.ingestion = ingestion;
    this.tenantScope = tenantScope;
  }

  @RabbitListener(queues = SyllabusIngestionQueueConfig.INGESTION_QUEUE)
  public void onIngestion(SyllabusIngestionMessage message) {
    tenantScope.runAsTenant(message.schoolId(), null, () -> ingestion.ingest(message.syllabusId()));
  }
}
