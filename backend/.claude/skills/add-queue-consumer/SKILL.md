---
name: add-queue-consumer
description: Add or change a RabbitMQ consumer for one of AcademiX's 3 AI-pipeline queues (homework.submissions.queue, homework.generation.queue, exam.submissions.queue), matching the exact retry/DLX/TTL topology in academix_backend_tdd.md. Use whenever wiring async job processing for submissions, generation, or exam grading. Trigger for "add a queue consumer", "wire the RabbitMQ listener", "queue worker", "async job processing".
---

# Add an AcademiX queue consumer

## The three queues — don't blur their configs together

| Queue | Job | Retry | TTL |
|---|---|---|---|
| `homework.submissions.queue` | OCR + combined grading/plagiarism Qwen call | 3x → DLX | 10 min |
| `homework.generation.queue` | Per-student unique task generation (2-layer verify) | 3x → DLX | (not separately specified — confirm with user rather than assume it matches submissions' 10min) |
| `exam.submissions.queue` | Bulk exam OCR+grading, no plagiarism | 3x → DLX | 15 min (longer — bulk uploads spike 30+ messages at once) |

## Steps

1. Confirm which queue this consumer is for and use its exact retry×3 → dead-letter-exchange, TTL from the table above — don't invent a different retry count or reuse another queue's TTL.
2. **Budget check happens inside the consumer, before the AI call** — not at the point of message publish. A message sitting in queue doesn't consume budget; the actual Qwen/Vision call does. See the `wire-ai-integration` skill for the budget-check and graceful-degradation rules (never hard-fail the message; on exhausted budget, homework grading degrades to `AI_SKIPPED` and the message is still acked as successfully processed).
3. **State machine transitions** the consumer is responsible for: `SUBMITTED → AI_PROCESSING` (on dequeue) → `AI_DONE` or `AI_SKIPPED` (on completion) — never leave a submission stuck in `AI_PROCESSING` on consumer failure; a DLX-routed message after 3 failed retries should still resolve the submission to some terminal-ish state the teacher can see and act on, not just vanish into the dead-letter exchange silently.
4. **Circuit breaker**: the consumer's Qwen call must go through the Resilience4j circuit breaker (50% failure rate or 75% slow-call rate >10s over a 10-call window trips it, 60s open state) — don't call Qwen directly from inside the consumer bypassing whatever wraps this.
5. **RLS**: the consumer runs outside an HTTP request context, so there's no JWT to derive `school_id` from automatically — make sure the consumer explicitly sets `SET LOCAL app.current_school_id` from the message payload (which must carry `schoolId`) before touching any RLS-scoped table, or every query in the consumer will silently return nothing (RLS defaults deny, not allow).

## After writing

Suggest `write-integration-test` for the retry/DLX/TTL behavior specifically — this is exactly the kind of timing-dependent behavior a mock would paper over.
