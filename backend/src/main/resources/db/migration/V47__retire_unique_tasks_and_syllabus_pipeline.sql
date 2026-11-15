-- Per-student unique tasks and the syllabus/lesson-plan pipeline were removed from the product
-- (MVP focus: AI grading of handwritten work). Their tables are intentionally kept — no data is
-- dropped here — but rows that the remaining code would misread are normalised.

-- Unique-task assignments become ordinary assignments: the code no longer reads `type` or
-- `tasks_published`, so a hidden (unpublished) unique-task assignment would otherwise surface with
-- no per-student task. Students now see the assignment's own description instead.
UPDATE homework_assignments
SET type = 'STANDARD',
    tasks_published = TRUE
WHERE type = 'UNIQUE_GENERATED'
   OR tasks_published = FALSE;

-- The outbox dispatcher no longer knows the syllabus-ingestion payload. Pending rows would retry
-- forever, so they are closed out (kept for audit, not deleted).
UPDATE outbox_events
SET published_at = CURRENT_TIMESTAMP,
    last_error = 'retired: syllabus ingestion pipeline removed'
WHERE payload_type = 'syllabusIngestion'
  AND published_at IS NULL;
