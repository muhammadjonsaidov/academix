---
name: deploy-release
description: Deploy or roll back AcademiX production (docker-compose on the VPS via GitHub Actions CD and scripts/deploy-vps.sh). Use when actually releasing or recovering, after pre-launch-checklist. Trigger for "deploy", "release", "roll back", "rollback the deploy".
---

# AcademiX deploy / rollback

## How a release ships
1. Merge to `main` → **CI** (`.github/workflows/ci.yml`: backend compile + `spotlessCheck` + Testcontainers tests, frontend lint/typecheck/test/build, Playwright route-guard E2E, telegram-bot build).
2. CI green → **CD** (`cd.yml`) runs on the self-hosted VPS runner and executes `scripts/deploy-vps.sh <sha>`:
   - refuses to run if the server checkout has modified tracked files;
   - validates `infra/.env` secrets;
   - `git checkout <sha>`, then `docker compose (base + production + tunnel overlays) up -d --build`;
   - restarts the ingress nginx and waits for a healthy stack;
   - **if anything fails it automatically redeploys the previous revision.**
3. Flyway migrations run on backend startup.

## Before deploying
- Take a database backup if the release has migrations (the auto-rollback reverts code, not schema/data). Destructive migrations need an explicit OK.
- Check `infra/.env` on the server for new variables in this release (e.g. `ACADEMIX_FEATURE_PSYCHOLOGY`, which also controls the frontend build). `NEXT_PUBLIC_*` values are baked in at build time.

## Manual rollback
On the server: `bash scripts/deploy-vps.sh <previous-good-sha>` (the last success is in `.deployed-revision`). If a migration must be undone, restore the pre-deploy backup — Flyway does not roll back.

## After deploying
- Health: `/actuator/health/readiness` (backend), the login page (frontend), and the bot polling log.
- Watch the first minutes: AI error rate, queue depth, DB pool, CPU.
- If the release touched tenant tables, spot-check data as a real school user — RLS mistakes show up as empty results, not errors.
