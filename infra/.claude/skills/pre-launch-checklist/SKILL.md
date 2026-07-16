---
name: pre-launch-checklist
description: Run through AcademiX's go-live checklist before a production release or launch — env vars, migrations, RLS, AI budget config, observability, retention jobs, rollback readiness. Use when the user says "are we ready to launch/ship/deploy", "pre-launch check", "go-live checklist", or before a release to production.
---

# AcademiX pre-launch checklist

Work through each item against the actual repo state — don't just recite the list, check it (grep for the config, read the actual migration/env files, confirm the job/policy exists). Report each item as done / missing / can't verify, don't mark anything done on assumption.

## Config & secrets
- [ ] All backend env vars set and non-default in the target environment: `SERVER_PORT`, `DATABASE_URL`/`DATABASE_USERNAME`/`DATABASE_PASSWORD`, `JWT_SECRET`, `REDIS_HOST`/`REDIS_PORT`, `QWEN_API_KEY`, `QWEN_BASE_URL`, `QWEN_MODEL_TEXT`, `QWEN_MODEL_VISION`, `GOOGLE_VISION_API_KEY`, `SEAWEEDFS_S3_ENDPOINT`/`ACCESS_KEY`/`SECRET_KEY`, `RABBITMQ_HOST`/`RABBITMQ_PORT`.
- [ ] Frontend `NEXT_PUBLIC_*` vars set: `NEXT_PUBLIC_API_URL` (pointing at production backend, not localhost), `NEXT_PUBLIC_TELEGRAM_BOT_USERNAME`, `NEXT_PUBLIC_SENTRY_DSN`.
- [ ] No hardcoded credentials anywhere (the `secrets-guard` hook helps catch new ones, but do a final `grep` sweep too — it only checks content at write-time, not pre-existing code).
- [ ] `JWT_SECRET` is a real production secret, not a dev placeholder.

## Database
- [ ] All Flyway migrations applied cleanly against a fresh schema (not just incrementally against a dev DB that may have manual patches).
- [ ] RLS enabled + `school_isolation` policy present on exactly the 9 spec-listed tables — run the `rls-auditor` agent rather than checking by hand.
- [ ] `BYPASSRLS` superadmin role confirmed inactive/commented out in production.
- [ ] **Production `spring.datasource.*` actually connects as the restricted `academix_app` role, not the migration superuser.** This is not optional — RLS is completely inert for a superuser/BYPASSRLS connection, confirmed by a real test. Don't trust that it's "probably fine because it works locally" — locally it works precisely because dev defaults match; verify the actual prod env vars (`DATABASE_APP_USERNAME`/`DATABASE_APP_PASSWORD`) are set and distinct from the Flyway superuser credentials.
- [ ] `uuid-ossp` and `vector` (pgvector) extensions installed; `ivfflat` index on `handwriting_profiles.feature_vector` exists.
- [ ] Backup taken immediately before migration, per the rollback runbook.
- [ ] Nightly `pg_dump` backup container running (7-day rolling retention) — separate from the pre-migration backup, this is routine disaster recovery. Confirm it's actually scheduled and a restore has been test-run at least once, not just configured.

## AI pipeline
- [ ] `monthlyAiCallLimit` set per school (not zero/unset) and the 15/65/20 exam/homework/chat split is actually enforced, not just documented.
- [ ] Resilience4j circuit breaker config matches spec: 50% failure rate or 75% slow-call (>10s) over 10 calls, 60s open state.
- [ ] RabbitMQ queues exist with correct retry/DLX/TTL: `homework.submissions.queue` (10min TTL), `homework.generation.queue`, `exam.submissions.queue` (15min TTL), all retry×3 → DLX.
- [ ] Admin AI-budget-low notification (80% threshold) actually fires, not just modeled in the entity.

## Security
- [ ] Nginx terminates TLS in front of both containers, cert auto-renewal via Let's Encrypt/Certbot actually configured (not just a one-time manual cert), AES-256 at rest for sensitive columns.
- [ ] `BCryptPasswordEncoder` strength 12.
- [ ] Telegram webhook validates `X-Telegram-Bot-Api-Secret-Token`; link tokens are single-use with 5-min Redis TTL and rate-limited 5/hour/user.
- [ ] Rate limiting (Bucket4j + Redis) actually enforced on rate-limited endpoints, not just present in a dependency list — confirm `ERR_RATE_LIMIT` actually fires under load, not only documented.
- [ ] Run the `security-reviewer` agent against the release diff, not just this checklist.

## Privacy / retention
- [ ] Data-deletion-request approval flow actually nulls `handwriting_profiles.feature_vector` and `psychological_signals.raw_evidence` (row kept for audit).
- [ ] Monthly job for 2-year psych-signal anonymization is scheduled, not just written.
- [ ] Weekly handwriting-reset anomaly-detection job is scheduled.

## Observability
- [ ] Structured JSON logging wired for ELK.
- [ ] Prometheus/Grafana alerts configured at the documented thresholds: CPU >85%/5min, DB pool <10% free, AI error rate >10%/100req, submission queue depth >50.
- [ ] `/health` endpoint responds correctly (referenced in the rollback runbook's verification step).

## Rollback readiness
- [ ] Previous stable Docker image tag identified and pullable (`registry.academixai.uz/backend:<stable-tag>`).
- [ ] Flyway rollback path checked/tested, not just assumed to work.
- [ ] Rollback runbook steps are current and someone other than the release author has read them.

## CI/CD
- [ ] GitHub Actions workflow runs backend tests (JUnit 5 + Testcontainers), frontend tests (Vitest) and E2E (Playwright), and lint/format checks (Spotless/google-java-format, ESLint/Prettier) on every PR before merge to the release branch.
- [ ] Workflow blocks merge on failure — a green check is required, not advisory.

## Known spec gaps to double check before launch (not covered by the docs, verify manually)
- No OpenAPI/codegen — frontend types are hand-written; run `frontend-contract-auditor` and do a manual pass confirming they still match `academix_tz.md` §2 before launch, since drift here won't show up as a build failure. springdoc-openapi (if wired) is a reference aid, not a substitute for this check.
- Secrets are plain `.env` + platform secrets (decided, not Vault) — confirm the target platform's secret store is actually used in production, not a checked-in `.env` file.
- Password reset: if implemented, confirm `academix_tz.md` §2.1 was actually updated first (not just coded off the CLAUDE.md decision) — and confirm students, who have no email field, have *some* recovery path before launch, even if it's manual/admin-assisted for now.
