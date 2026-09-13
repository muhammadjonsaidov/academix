# AcademiX AI

A SaaS platform for schools in Uzbekistan that uses AI to grade handwritten homework and exams,
auto-generates per-student unique homework variants, tracks handwriting biometrics to catch
submission fraud, runs silent psychological well-being monitoring on student activity, and
gamifies homework via XP/streaks/badges.

Five roles, five dashboards: **Admin**, **Teacher**, **Student**, **Parent**, **Psychologist**.

## Status

Sprints 1–14 done — auth, admin/teacher/student/parent/psychologist CRUD and dashboards, the full
AI homework/exam grading pipeline, unique-task generation, gamification, handwriting biometrics,
psychological signal monitoring, Telegram bot + notifications, admin analytics, Jasper PDF
reports, and the AI Tutor chat are all built and verified live end-to-end. What's left
(talent-analysis and password reset) is blocked pending further input, not actionable yet.

## Stack

- **Backend**: Java 21, Spring Boot 4.1, Spring AI, Gradle, PostgreSQL 18 + pgvector, Redis,
  RabbitMQ, Flyway, Resilience4j
- **Frontend**: Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS 4, shadcn/ui
  (Base UI), Zustand, Axios
- **Storage**: SeaweedFS (S3-compatible)
- **AI**: Google Cloud Vision (OCR) and any OpenAI-compatible provider (grading, plagiarism, chat,
  psychology signals, generation)
- **PDF reports**: JasperReports
- **Bot**: Telegram

Exact pinned versions and every real integration finding from building this (dependency quirks,
framework gotchas, config that actually works vs. what the docs imply) live in the internal
`CLAUDE.md` — see "Documentation" below.

## Repository layout

```
backend/       Java 21 + Spring Boot + Gradle project (DDD/Clean Architecture)
frontend/      Next.js + TypeScript project (App Router)
telegram-bot/  separate Java 21 + Spring Boot service — own build, own container, own .env
infra/         docker-compose.yml, nginx, postgres-init, seaweedfs-config, demo seed
.claude/       shared tooling — hooks, cross-cutting agents/skills
```

Backend follows `domain → application → infrastructure ← interfaces/web`. Frontend is type-based
at the top level (`app/`, `components/`, `stores/`, `types/`), role-scoped only where components
genuinely differ per role.

## Getting started

### 1. Environment

```
cp backend/.env.example backend/.env
cp frontend/.env.local.example frontend/.env.local
cp telegram-bot/.env.example telegram-bot/.env
```

Fill in the real secrets in `backend/.env` (`AI_API_KEY`, `AI_BASE_URL`, `AI_MODEL_TEXT`,
`GOOGLE_VISION_API_KEY`,
`TELEGRAM_BOT_TOKEN`) — everything else already has working local-dev defaults.
`TELEGRAM_BOT_TOKEN` must be set to the **same value** in `telegram-bot/.env`: both services act
as the same bot identity. The app boots and
degrades gracefully without the AI/Telegram keys (submissions still accept, just skip AI
grading/delivery) — see the internal `CLAUDE.md`'s graceful-degradation notes.

`JWT_SECRET` in `backend/.env` and `frontend/.env.local` **must be byte-for-byte identical** —
different values silently break every authenticated request.

### 2. Full stack in Docker — one command (recommended)

```
cp infra/.env.example infra/.env   # optional: real AI/Telegram secrets
cd infra
docker compose up -d --build
```

Brings up the **whole product**: Postgres (with pgvector) + Redis + RabbitMQ + SeaweedFS,
backend (`http://localhost:8080`), frontend (`http://localhost:3000`), telegram-bot, and a
one-shot `seed` service that fills the DB with the demo data (`infra/seed/bootstrap.sql` first,
then `infra/seed/demo-seed.sql`).

Demo logins (password `Test1234!` for all) — full list in `infra/seed/demo-seed.sql`:

| Role        | Phone          |
|-------------|----------------|
| Admin       | +998901234567  |
| Teacher     | +998911112233  |
| Student     | +998933334455  |
| Parent      | +998977001122  |
| Psychologist| +998955501234  |

Optional extras, still profile-gated:

```
docker compose --profile antivirus up -d clamav   # upload virus scanning (needs ACADEMIX_CLAMAV_ENABLED=true)
docker compose --profile prod up -d               # nginx TLS + nightly postgres backup
```

### 3. Backend (local dev, no Docker)

```
cd backend
./gradlew bootRun     # http://localhost:8080
./gradlew test         # JUnit 5 + Testcontainers
```

**`./gradlew test` needs the infra stack running** (postgres/redis/rabbitmq/seaweedfs) —
`docker compose up -d` in `infra/` starts those along with the app; to run only the infra
services (e.g. for local `./gradlew bootRun`/`npm run dev`), comment out the app services in
`infra/docker-compose.yml`. Specifically SeaweedFS: Testcontainers starts Postgres/RabbitMQ/Redis itself, but
`FileStorageService` reaches for the S3 endpoint from `@PostConstruct`, so without it the Spring
context fails to start and 18 of 50 tests fail with a misleading `ApplicationContext failure
threshold exceeded` that names no root cause.

### 4. Frontend (local dev, no Docker)

```
cd frontend
npm install
npm run dev            # http://localhost:3000
npm run test
```

### 5. Telegram bot

With Docker, telegram-bot comes up together with everything else (works with or without
`TELEGRAM_BOT_TOKEN` — no token = bot skips polling, the app still works).

Running it standalone (no Docker):

```
cd telegram-bot
./gradlew bootRun      # http://localhost:8081
```

## Documentation

All of the following are **kept locally and deliberately not committed** (see `.gitignore`) — they
exist in a working checkout but not on GitHub, so they're listed here by name rather than linked:

- `CLAUDE.md` — architecture, stack decisions, every real bug/deviation found while building
  this, and the conventions any future change should follow
- `ROADMAP.md` — sprint-by-sprint build history and what's left
- `academix_tz.md` / `academix_backend_tdd.md` / `academix_frontend_tdd.md` — the original spec
  docs, source of truth for every API/enum/queue contract
