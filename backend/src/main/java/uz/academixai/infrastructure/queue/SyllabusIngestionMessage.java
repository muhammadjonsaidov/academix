package uz.academixai.infrastructure.queue;

import java.util.UUID;

/**
 * Minimal, idempotent work message; all source metadata is read from the database by the worker.
 */
public record SyllabusIngestionMessage(UUID schoolId, UUID syllabusId) {

  // schoolId added alongside the Learning migration: the ingestion worker has no HTTP request to
  // derive a tenant from, and the tables it writes (teacher_syllabuses, syllabus_chunks) are
  // scheduled for RLS. Messages queued before that deploy carry no schoolId and are rejected by
  // the listener's scope rather than processed without one — re-upload the syllabus.
}
