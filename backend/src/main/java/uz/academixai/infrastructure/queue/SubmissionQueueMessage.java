package uz.academixai.infrastructure.queue;

import java.util.UUID;

/** Minimal queue payload — the consumer re-reads the full submission from Postgres by id. */
public record SubmissionQueueMessage(UUID submissionId) {}
