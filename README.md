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
reports, and the AI Tutor chat are all built and verified live end-to-end. See
[`ROADMAP.md`](./ROADMAP.md) for the full sprint-by-sprint history and what's left (talent-analysis
and password reset — both blocked pending further input, not actionable yet).

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

See [`CLAUDE.md`](./CLAUDE.md) for exact pinned versions and every real integration finding from
building this (dependency quirks, framework gotchas, config that actually works vs. what the docs
imply).

## Repository layout

```
backend/     Java 21 + Spring Boot + Gradle project (DDD/Clean Architecture)
frontend/    Next.js + TypeScript project (App Router)
infra/       docker-compose.yml, deployment config
.claude/     shared tooling — hooks, cross-cutting agents/skills
```

Backend follows `domain → application → infrastructure ← interfaces/web`. Frontend is type-based
at the top level (`app/`, `components/`, `stores/`, `types/`), role-scoped only where components
genuinely differ per role.

## Getting started

### 1. Environment

```
cp backend/.env.example backend/.env
cp frontend/.env.local.example frontend/.env.local
```

Fill in the real secrets in `backend/.env` (`QWEN_API_KEY`, `GOOGLE_VISION_API_KEY`,
`TELEGRAM_BOT_TOKEN`) — everything else already has working local-dev defaults. The app boots and
degrades gracefully without the AI/Telegram keys (submissions still accept, just skip AI
grading/delivery) — see `CLAUDE.md`'s graceful-degradation notes.

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

### 4. Frontend

```
cd frontend
npm install
npm run dev            # http://localhost:3000
npm run test
```

## Documentation

- [`CLAUDE.md`](./CLAUDE.md) — architecture, stack decisions, every real bug/deviation found
  while building this, and the conventions any future change should follow
- [`ROADMAP.md`](./ROADMAP.md) — sprint-by-sprint build history and what's left
- `academix_tz.md` / `academix_backend_tdd.md` / `academix_frontend_tdd.md` — the original spec
  docs (source of truth for contracts; not committed to this repo, see `.gitignore`)
