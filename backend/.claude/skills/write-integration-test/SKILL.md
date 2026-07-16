---
name: write-integration-test
description: Write an integration test for AcademiX backend code that crosses a real boundary — RLS-scoped DB queries, RabbitMQ queue consumers, Redis budget counters, or external AI vendor calls (Qwen/Google Vision). Use for anything beyond a pure unit test. Trigger for "write an integration test", "test with a real database", "test the queue consumer", "test RLS", "test the AI budget check".
---

# Write an AcademiX integration test

## Framework

Decided (CLAUDE.md "Supporting tooling"): **JUnit 5 + Testcontainers + Mockito**. Not spec-mandated by the TDDs, but locked in — don't re-litigate or ask per PR. If `build.gradle` disagrees with this once the project exists, that's drift worth flagging, not a cue to follow the code over the decision.

For frontend-side API-contract tests: **Vitest**, per the same decision.

## What's worth an integration test here (vs a unit test)

- **RLS enforcement** — the single highest-value integration test category in this codebase, precisely because CLAUDE.md flags RLS as the thing an app-level bug could silently bypass. Test: two schools' data seeded, query as school A's `current_school_id`, assert school B's rows never appear — run against a real Postgres (Testcontainers), not a mock, since RLS is a DB-level feature a mock can't simulate.
- **Queue consumers** — `homework.submissions.queue`/`homework.generation.queue`/`exam.submissions.queue` behavior: message → OCR/grading call → status transition (`SUBMITTED → AI_PROCESSING → AI_DONE|AI_SKIPPED`), retry×3 → DLX on persistent failure, TTL expiry. Use a real RabbitMQ (Testcontainers) — timing/retry/DLX behavior is exactly what a mock would paper over.
- **AI budget counters** — Redis-backed `ai_budget_{category}:{schoolId}:{yyyy-mm}` increments and the exam/homework/chat split (15/65/20), plus the degradation order (chat blocks first, exam last) when a budget is exhausted. Real Redis (Testcontainers), not a mock — the row-lock-avoidance rationale for using Redis over Postgres is itself worth verifying under concurrent increments.
- **External AI vendor calls** — stub Qwen/Google Vision with WireMock, not a mocked Java client, so the actual HTTP contract (request shape per TZ §3, response parsing, circuit-breaker trip behavior on failure/slow-call thresholds) is exercised. Don't hit the real Qwen/Vision APIs in tests — cost and flakiness both matter here (AI budget system exists precisely because these calls cost money).
- **Data-deletion request flow** — approval nulls `feature_vector`/`raw_evidence` but keeps the row; worth a real-DB test since a subtly wrong `UPDATE` vs `DELETE` here is a privacy bug, not just a functional one.

## What's still fine as a unit test

Pure calculation logic (`aiScorePercent` weighted-sum from `criteriaScores`), XP tier/streak math, the jailbreak-response heuristic (short-answer detection) — no real boundary crossed, mock/stub freely.

## Conventions

- AAA pattern (arrange/act/assert), descriptive naming (`shouldSkipGrading_whenHomeworkBudgetExhausted`), isolate each test's tenant data (don't let one test's school leak into another's RLS assertions), keep Testcontainers instances scoped for reasonable parallelization rather than one shared container serializing the whole suite.

## After writing

If the test surfaces a spec ambiguity (e.g. exact retry timing not specified), that's worth flagging to the user rather than guessing and asserting on an invented number.
