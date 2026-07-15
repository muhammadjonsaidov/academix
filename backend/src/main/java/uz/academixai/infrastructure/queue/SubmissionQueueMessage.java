package uz.academixai.infrastructure.queue;

import java.util.UUID;

/**
 * Carries {@code schoolId} alongside {@code submissionId} — not just for convenience. {@code
 * homework_submissions} is RLS-enabled, and the listener has no HTTP request to derive it from
 * (unlike every other RLS-touching code path, which goes through {@code RlsTransactionFilter}). The
 * listener needs {@code schoolId} to run its own {@code SET LOCAL app.current_school_id} before it
 * can even read the submission row by id — see {@link HomeworkSubmissionListener}.
 */
public record SubmissionQueueMessage(UUID submissionId, UUID schoolId) {}
