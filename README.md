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
- **AI**: Google Cloud Vision (OCR), Alibaba Qwen via DashScope (grading, plagiarism, chat,
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

Fill in the real secrets in `backend/.env` (`QWEN_API_KEY`, `GOOGLE_VISION_API_KEY`,
`TELEGRAM_BOT_TOKEN`) — everything else already has working local-dev defaults.
`TELEGRAM_BOT_TOKEN` must be set to the **same value** in `telegram-bot/.env`: both services act
as the same bot identity. The app boots and
degrades gracefully without the AI/Telegram keys (submissions still accept, just skip AI
grading/delivery) — see the internal `CLAUDE.md`'s graceful-degradation notes.

`JWT_SECRET` in `backend/.env` and `frontend/.env.local` **must be byte-for-byte identical** —
different values silently break every authenticated request.

### 2. Infra

```
cd backend
docker-compose -f ../infra/docker-compose.yml up -d
```

Brings up Postgres (with pgvector), Redis, RabbitMQ, SeaweedFS.

### 3. Backend

```
cd backend
./gradlew bootRun     # http://localhost:8080
./gradlew test         # JUnit 5 + Testcontainers
```

**`./gradlew test` needs step 2's stack running** — specifically SeaweedFS. Testcontainers starts
Postgres/RabbitMQ/Redis itself, but `FileStorageService` reaches for the S3 endpoint from
`@PostConstruct`, so without it the Spring context fails to start and 18 of 50 tests fail with a
misleading `ApplicationContext failure threshold exceeded` that names no root cause.

### 4. Frontend

```
cd frontend
npm install
npm run dev            # http://localhost:3000
npm run test
```

### 5. Telegram bot (optional)

Its own Gradle build — starting the backend does **not** start it. Skip this and everything works
except Telegram delivery and the `/start` link flow.

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
