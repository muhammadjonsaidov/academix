package uz.academixai.learning.application.port.out;

import java.util.UUID;

/**
 * Asks the knowledge-indexing worker to process an uploaded syllabus.
 *
 * <p>The message carries the school because the worker has no HTTP request to derive a tenant from
 * — the same reason submission analysis messages carry one.
 */
public interface SyllabusIngestionPublisher {

  void requestIngestion(UUID schoolId, UUID syllabusId);
}
