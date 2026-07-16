---
name: deploy-release
description: Execute an AcademiX production deploy or rollback, following the exact runbook in academix_backend_tdd.md. Use when actually deploying/releasing, not just checking readiness (see pre-launch-checklist for the readiness check that should happen first). Trigger for "deploy", "release", "roll back", "rollback the deploy".
---

# AcademiX deploy / rollback

Run `pre-launch-checklist` first if this is a first launch or a release with schema/config changes — this skill is about executing the deploy, not verifying readiness.

**Prerequisite**: `infra/docker-compose.yml`'s `backend`/`frontend`/`nginx`/`certbot` services are commented out by default (Docker builds are slow, not needed for routine local iteration — see the file's header comment). Uncomment that whole block (and the `certbot-www`/`certbot-certs` volumes at the bottom) before running any of the steps below — they don't exist in the default config.

## Both backend and frontend deploy the same way

Decided (CLAUDE.md "Supporting tooling"): **plain `docker-compose` in production, for both backend and frontend**, no Kubernetes/blue-green/canary — deliberately not building for scale this project doesn't have yet. **This supersedes `academix_frontend_tdd.md`'s Vercel/Cloudflare Pages deploy/rollback section** — that describes a platform-native deploy this project isn't using anymore; a Next.js production build gets containerized like everything else, not shipped to Vercel/Cloudflare.

## Rollback runbook (backend TDD, applies to both services)

1. Stop the current container (`backend` or `frontend`).
2. `docker pull registry.academixai.uz/<backend|frontend>:<stable-tag>` — pull the last known-good tag, don't guess a version.
3. `docker-compose up -d --no-deps <backend|frontend>` — restart only the affected service, don't cycle dependencies (DB/Redis/RabbitMQ, or the other service) unnecessarily.
4. Backend only: check whether the schema changed; if so, check the Flyway rollback and restore the pre-migration backup taken beforehand (see `create-migration`'s note on writing migrations that are actually reversible in practice). Frontend has no equivalent migration step.
5. Verify via `/health` (backend) or a basic reachability check (frontend — confirm the built pages actually serve, not just that the container is running) before declaring the rollback complete.

## Forward deploy

Procedure, same for both services: pull the new image → `docker-compose up -d --no-deps <service>` → verify (`/health` for backend, reachability for frontend) → watch the Grafana alert thresholds (CPU >85%/5min, DB pool <10% free, AI error rate >10%/100req, queue depth >50) for the first several minutes. This is the standing decision, not a default to re-confirm each release.

CI (GitHub Actions) builds and pushes both images and gates the deploy — a deploy should only ever pull an image built from a commit that passed tests/lint on the release branch, not an arbitrary local build. Frontend build (`npm run build`) happens inside the image build step, same as any other containerized Next.js deploy — `NEXT_PUBLIC_*` env vars must be baked in at build time, not just set at container runtime, since Next.js inlines them at build.

## Before declaring a deploy done

- `/health` returns healthy.
- No spike against the Prometheus/Grafana thresholds above in the first few minutes post-deploy.
- If this deploy included a migration, confirm no RLS regression — a migration bug here fails silently (queries return empty rather than erroring) rather than loudly, so don't rely on "no errors in the logs" as proof it worked. Run `rls-auditor` if the migration touched a school_id-bearing table.

## Don't

- Don't skip the pre-migration backup step to save time — the whole rollback path depends on it existing.
- Don't restart dependency containers (Postgres/Redis/RabbitMQ) as part of a routine single-service deploy; that's a bigger blast radius than the change usually warrants.
- Don't follow `academix_frontend_tdd.md`'s Vercel/Cloudflare deploy instructions literally — that section is superseded by the docker-compose decision above.
- Don't ship a frontend image with `NEXT_PUBLIC_API_URL` (or other `NEXT_PUBLIC_*` vars) pointing at localhost/dev values baked in from a stale build cache — since these are compile-time-inlined, a wrong value here silently ships to production and can't be fixed by changing runtime env vars.
