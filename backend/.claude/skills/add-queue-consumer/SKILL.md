---
name: add-queue-consumer
description: Add or change asynchronous work in the AcademiX backend — RabbitMQ queues, listeners, the transactional outbox, retry/DLX/TTL. Use for homework/exam AI processing, notification delivery, or any new background job. Trigger for "add a queue consumer", "RabbitMQ listener", "queue worker", "async job processing", "outbox".
---

# AcademiX async work

## Current queues
| Queue | Job | Retry | TTL |
|---|---|---|---|
| `homework.submissions.queue` | OCR + one merged grading/plagiarism AI call | 3x → DLX | 10 min |
| `exam.submissions.queue` | Bulk exam OCR + grading (no plagiarism) | 3x → DLX | 15 min |
| notification Telegram queue | Delivery, consumed by the separate `telegram-bot` service | 3x → DLQ | — |

Config lives in `infrastructure/queue/*QueueConfig`; pick retry/TTL deliberately for a new queue and document it in `academix_backend_tdd.md`.

## How to publish
Never publish inside the request transaction. Write an `outbox_events` row through `OutboxService` in the same transaction as the data; `OutboxDispatcher` publishes after commit (retry with backoff). Add your payload type to the dispatcher's switch.

## How to consume
1. The message carries `schoolId`; enter `TenantScope.runAsTenant(schoolId, …)` before touching any table — there's no HTTP request to set RLS scope, and an unscoped query on an RLS table fails loudly (`invalid input syntax for type uuid: ""`).
2. Make it idempotent: claim the work atomically (see `claimForAnalysis`) so a redelivery is a no-op.
3. Budget checks happen in the consumer, right before the AI call (see `wire-ai-integration`). Exhausted budget → `AI_SKIPPED`, message acked.
4. Status flow: `SUBMITTED → AI_PROCESSING → AI_DONE | AI_SKIPPED`; never leave work stuck in `AI_PROCESSING`.
5. AI/Vision calls go through the Resilience4j-wrapped clients, not raw HTTP.

## Test it
Use Testcontainers (real RabbitMQ/Postgres) — see `write-integration-test`.
