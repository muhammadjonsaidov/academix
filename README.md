# AcademiX AI

**Live:** [academixai.uz](https://academixai.uz)

AcademiX AI gives teachers in Uzbek schools back the hours they spend checking notebooks. A student
(or the teacher) photographs a handwritten page; the AI reads it, checks it the way that subject is
checked, flags copied or machine-written work and suggests a score. **The teacher always gives the
final grade.** Parents see the grade and the teacher's comment on their phone, in Telegram.

What we sell is teachers' time and parents' trust. Every feature is judged by those two questions.

## The problem

A teacher in Uzbekistan spends 6–8 hours a week checking notebooks — the same arithmetic error, the
same spelling mistake, thirty times over. Parents see a number in an electronic journal weeks later,
with no reason attached. Existing tools are journals (Kundalik, my.maktab.uz) or don't read
handwriting (Google Classroom).

## How it works

1. **Photo.** The student photographs the notebook page (typed answers work too).
2. **The AI checks it, in one of three modes picked from the subject:**

   | Mode | Subjects | What the teacher sees |
   |---|---|---|
   | Problem solving | Mathematics, physics, chemistry | Each step marked right or wrong, with the correct step |
   | Language | Uzbek, Russian, English | Every spelling, grammar and punctuation error with its correction |
   | Written response | History, biology, geography, literature, other | Each claim checked for accuracy and completeness |

   The same call scores the teacher's own rubric criteria and checks originality. The AI never sets
   the grade: it returns per-criterion scores and the backend computes a suggested percentage.
3. **The teacher confirms.** The home screen is a review queue: flagged work first, oldest next.
   Clean work for an assignment is confirmed in one click; anything else opens side by side with
   the student's photo, and the 5-point grade is suggested from the score.
4. **Parents and students are told.** A confirmed grade reaches the student and their parents
   (inbox + Telegram); new assignments and next-day deadlines send reminders.

Exams work the same way in bulk: the teacher uploads a stack of scanned papers after a proctored test.

**Degrades, never blocks.** Each school has a monthly AI allowance split into exam (15%), homework
(65%) and tutor chat (20%). Admins are warned at 80%. When a budget runs out, work is still accepted
and lands in the teacher's "grade by hand" tab — nothing is lost.

## Business model

Schools pay per month by size (VAT and contract terms in `BIZNES_MODEL.md`):

| Plan | Students | AI checks / month | Price / month |
|---|---|---|---|
| Starter | up to 150 | 4 000 | 1 800 000 so'm |
| Standard | up to 400 | 12 000 | 4 000 000 so'm |
| Pro | up to 900 | 30 000 | 7 200 000 so'm |

Sales start with a paid pilot (one quarter, two classes, one subject, 50% off) measured against
KPIs agreed up front. Add-ons: a psychological well-being module, exam packs, Parent Premium.

## Roles

| Role | Main screen |
|---|---|
| Teacher | Review queue, homework and exams, class analytics, own grading criteria |
| Student | What is due (deadline in words), results with every mistake explained, XP, streaks, badges, an AI tutor that won't hand out answers |
| Parent | Each child's latest confirmed grade, homework still due, Telegram link |
| Admin (director) | School setup and Excel import, teachers, classes, analytics, quarterly PDF reports, AI usage |
| Psychologist | Only with the optional well-being module (off by default) |

## Design

Pencil and red pen on a squared school notebook: the AI's analysis is drawn in pencil (provisional),
only a grade the teacher confirmed appears as the red, circled numeral, and work that needs a human
look carries a yellow highlighter. Full system in [`docs/design.md`](docs/design.md).

## Architecture

```
backend/       Java 21, Spring Boot 4.1 — modular monolith (identity, school, learning,
               intelligence, progress, family, notification, reporting, optional wellbeing)
frontend/      Next.js 16 (App Router), React 19, TypeScript 5.9, Tailwind 4, Zustand
telegram-bot/  separate Spring Boot service: account linking, notification delivery
infra/         docker-compose (+ production / tunnel overlays), nginx, seed data
docs/          design system, tenancy (RLS), modular-monolith notes
```

- **Tenant isolation in the database:** PostgreSQL row-level security on tenant tables; the
  application connects as a restricted role and every unit of work declares its school.
- **AI is asynchronous:** RabbitMQ queues with a transactional outbox; retries go to a dead-letter
  queue. OCR is Google Cloud Vision; text models use any OpenAI-compatible endpoint (DeepSeek by
  default), behind a Resilience4j circuit breaker.
- **Storage:** SeaweedFS (S3-compatible); student photos are served only through an authenticated
  API.
- **Quality gates:** ~700 backend tests (JUnit 5, Testcontainers, ArchUnit rules for context
  boundaries and RLS coverage), Vitest and Playwright on the frontend, Spotless formatting.
- **Delivery:** GitHub Actions CI; every push to `main` deploys to the production server through a
  self-hosted runner, with health checks and automatic rollback.

## Running it

### Full stack in Docker

```
cp infra/.env.example infra/.env     # add AI / Vision / Telegram keys to enable those features
cd infra
docker compose --profile demo up -d --build
```

Starts PostgreSQL, Redis, RabbitMQ, SeaweedFS, the backend (`:8080`), frontend (`:3000`),
Telegram bot and a one-off seed with a sample school: five teachers across five subjects, two
classes, students, parents and checked work. Sample logins use the password `Test1234!` — the
`demo` profile exists only for local use and must never run in production.

| Role | Phone |
|---|---|
| Admin | +998901234567 |
| Teacher (mathematics) | +998911112233 |
| Teacher (Uzbek / English / history) | +998915207341 / +998916738420 / +998917462053 |
| Student | +998933334455 |
| Parent | +998977001122 |

Without AI keys the app still runs: submissions are accepted and teachers grade by hand.

### Pieces on their own

```
cd backend && ./gradlew bootRun        # JDK 21; tests: ./gradlew test (needs Docker + infra seaweedfs)
cd frontend && npm install && npm run dev
cd telegram-bot && ./gradlew bootRun   # :8081
```

`JWT_SECRET` must be byte-for-byte identical in `backend/.env` and `frontend/.env.local`, and
`TELEGRAM_BOT_TOKEN` the same in the backend and the bot.

### Optional well-being module

Psychological monitoring (nightly analysis, the Psychologist role) is a paid add-on and off by
default. `ACADEMIX_FEATURE_PSYCHOLOGY=true` in `infra/.env` switches it on for the backend, bot and
frontend build; while off its endpoints don't exist and psychologist accounts can't sign in.

## Documentation

- [`docs/design.md`](docs/design.md) — design system: concept, colour, type, layout, copy
- [`docs/architecture/tenancy.md`](docs/architecture/tenancy.md) — how row-level security is applied
- [`docs/architecture/ddd-modular-monolith.md`](docs/architecture/ddd-modular-monolith.md) — bounded contexts and their rules
- [`frontend/ARCHITECTURE.md`](frontend/ARCHITECTURE.md) — frontend structure
- [`BIZNES_MODEL.md`](BIZNES_MODEL.md) — business model, pricing, go-to-market (Uzbek)
