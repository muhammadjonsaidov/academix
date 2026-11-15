---
name: pre-launch-checklist
description: Go-live readiness check for AcademiX before a production release — secrets, database role/RLS, migrations, AI budget, optional modules, security, privacy jobs, observability, rollback. Trigger for "are we ready to launch/ship/deploy", "pre-launch check", "go-live checklist".
---

# AcademiX pre-launch checklist

Check each item against the real repo/server state and report done / missing / can't verify — never mark something done on assumption.

## Config & secrets
- [ ] `infra/.env` on the server: real `JWT_SECRET` (same value backend + frontend), DB superuser and `academix_app` passwords, `REDIS_PASSWORD`, RabbitMQ and SeaweedFS credentials, `AI_API_KEY`/`AI_BASE_URL`/`AI_MODEL_TEXT`, `GOOGLE_VISION_API_KEY`, `TELEGRAM_BOT_TOKEN`, Gmail SMTP. `deploy-vps.sh` validates the critical ones.
- [ ] `ProductionConfigurationValidator` passes (it refuses default secrets in production).
- [ ] Frontend build args point at production (`NEXT_PUBLIC_API_URL`, site URL, bot username).
- [ ] Optional modules decided per deployment: `ACADEMIX_FEATURE_PSYCHOLOGY` (backend + bot + frontend build), `ACADEMIX_CLAMAV_ENABLED=true` with the `antivirus` profile running.
- [ ] No secrets committed (`git grep` for keys/tokens).

## Database
- [ ] Runtime datasource is the restricted `academix_app` role, not the Flyway superuser (RLS is inert for superusers).
- [ ] Migrations apply cleanly on a fresh database (CI's Testcontainers run proves this) and on a copy of production data for this release.
- [ ] `TenantRlsCoverageTest` green; nothing new on its backlog without a reason.
- [ ] Nightly backup container running and a restore has actually been tested.
- [ ] Demo seed (`--profile demo`) is NOT used in production.

## AI pipeline
- [ ] Every school has `monthlyAiCallLimit` set. Known gap: the `AI_BUDGET_LOW` notification type exists but nothing sends it yet — decide whether launch needs it.
- [ ] Queues exist with retry/DLX/TTL (`homework.submissions.queue` 10 min, `exam.submissions.queue` 15 min); DLQs are empty or understood.
- [ ] Circuit breakers configured (50% failures / 75% slow over 10 calls, 60 s open).

## Security
- [ ] TLS via nginx/tunnel; only the ingress port is public (Postgres/Redis/RabbitMQ/SeaweedFS/backend stay internal).
- [ ] Refresh token only in the httpOnly cookie; auth cookies `Secure` + `SameSite=Strict`.
- [ ] Rate limits fire (`ERR_RATE_LIMIT`) for login, uploads, Telegram link tokens.
- [ ] Swagger not public (`SWAGGER_PUBLIC=false`).

## Privacy / retention
- [ ] Data-deletion approval nulls psychological evidence and leftover handwriting vectors (rows kept).
- [ ] Monthly 2-year psych-signal anonymisation job (`DataDeletionService`, 03:00 on the 1st) is running.

## Observability & rollback
- [ ] `/actuator/health/liveness` and `/readiness` reachable; logs shipped; alerts on CPU, DB pool, AI error rate, queue depth.
- [ ] Previous good revision known (`.deployed-revision`) and `deploy-release`'s rollback steps read by someone besides the author.
