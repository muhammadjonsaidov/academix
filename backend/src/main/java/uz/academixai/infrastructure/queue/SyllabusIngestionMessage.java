package uz.academixai.infrastructure.queue;

import java.util.UUID;

/**
 * Minimal, idempotent work message; all source metadata is read from the database by the worker.
 */
public record SyllabusIngestionMessage(UUID syllabusId) {}
