# AcademiX AI

A SaaS platform for schools in Uzbekistan that saves teachers grading time: a teacher (or student)
photographs a handwritten homework or exam, AI reads it, checks it step by step against the
teacher's criteria, flags copied or AI-generated work, and proposes a score — the teacher always
gives the final grade. Parents follow results through their dashboard and Telegram, and students
are motivated with XP, streaks and badges.

Four core roles: **Admin**, **Teacher**, **Student**, **Parent**. A fifth, **Psychologist**, comes
with the optional psychological well-being add-on module (off by default — see below).

## Status

Built and verified end-to-end: auth and password recovery, school setup and Excel bulk import,
the AI homework/exam grading pipeline (OCR + merged grading/plagiarism call, per-school AI budget
with graceful degradation), teacher review and final grading, gamification, parent dashboard,
Telegram bot + notifications, admin analytics, quarterly PDF reports and the AI Tutor chat.

Removed in the MVP-focus cleanup (2026-11): per-student unique task generation, handwriting
biometrics (and the parent biometric-consent flow), and the syllabus/lesson-plan pipeline. Their
database tables are still present but unused.

### Optional module: psychological monitoring

Nightly AI analysis of student activity, the Psychologist role and dashboard, and teacher signal
views. It is a paid add-on and **off by default**. Switch it on with one variable —
`ACADEMIX_FEATURE_PSYCHOLOGY=true` in `infra/.env` (it feeds the backend, the Telegram bot and the
frontend build). While off, its endpoints are not registered and Psychologist accounts cannot sign
in.

## Stack

- **Backend**: Java 21, Spring Boot 4.1, Gradle, PostgreSQL 18 (pgvector image), Redis, RabbitMQ,
  Flyway, Resilience4j
- **Frontend**: Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS 4, shadcn/ui
  (Base UI), Zustand, Axios
- **Storage**: SeaweedFS (S3-compatible)
- **AI**: Google Cloud Vision (OCR) and any OpenAI-compatible provider (grading + plagiarism,
  tutor chat, psychology signals)
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
docs/          architecture notes (modular monolith, tenancy/RLS, production roadmap)
```

Backend is a modular monolith: one bounded context per package (`identity`, `school`, `learning`,
`intelligence`, `progress`, `wellbeing`, `family`, `notification`, `reporting`), each with
`domain/`, `application/` (ports), `infrastructure/` and web adapters — boundaries are enforced by
ArchUnit tests. A shrinking legacy `domain/`, `infrastructure/` and `interfaces/web/` tree still
holds shared entities and most controllers. Frontend is type-based
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
docker compose --profile demo up -d --build
```

Brings up the **whole product**: Postgres + Redis + RabbitMQ + SeaweedFS,
backend (`http://localhost:8080`), frontend (`http://localhost:3000`), telegram-bot, and a
one-shot `seed` service that fills the DB with demo data (`infra/seed/bootstrap.sql` first, then
`infra/seed/demo-seed.sql`). The `demo` profile is deliberately required because it creates known
`Test1234!` credentials and must never be used for production.

Demo logins (password `Test1234!` for all) — full list in `infra/seed/demo-seed.sql`:

| Role        | Phone          |
|-------------|----------------|
| Admin       | +998901234567  |
| Teacher     | +998911112233  |
| Student     | +998933334455  |
| Parent      | +998977001122  |
| Psychologist| +998955501234 (only with `ACADEMIX_FEATURE_PSYCHOLOGY=true`) |

Optional extras, still profile-gated:

```
docker compose --profile antivirus up -d clamav   # upload virus scanning (needs ACADEMIX_CLAMAV_ENABLED=true)
docker compose --profile prod up -d               # nginx TLS + nightly postgres backup
```

For a non-demo local stack, omit `--profile demo`; provision the first school/admin through the
approved bootstrap procedure rather than demo credentials.

### 3. Backend (local dev, no Docker)

```
cd backend
./gradlew bootRun     # http://localhost:8080
./gradlew test         # JUnit 5 + Testcontainers
```

Use **JDK 21** (CI does). google-java-format, which `spotlessCheck` runs in CI, crashes under newer
JDKs; if you switched JDKs, force a fresh run with `./gradlew spotlessApply --rerun-tasks`.

**`./gradlew test` needs Docker plus SeaweedFS** — Testcontainers starts Postgres/RabbitMQ/Redis
itself, but `FileStorageService` reaches for the S3 endpoint at startup, so run
`docker compose up -d seaweedfs` in `infra/` first. Without it most integration tests fail with a
misleading `ApplicationContext failure threshold exceeded` that names no root cause.

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

- `CLAUDE.md` — architecture, stack decisions and the conventions any future change should follow
- `LESSONS.md` — every real bug/deviation found while building this (the "reality checks")
- `ROADMAP.md` — sprint-by-sprint build history and what's left
- `academix_tz.md` / `academix_backend_tdd.md` / `academix_frontend_tdd.md` — the original spec
  docs, source of truth for every API/enum/queue contract (their unique-task, handwriting and
  syllabus/lesson-plan sections describe features that have since been removed)
