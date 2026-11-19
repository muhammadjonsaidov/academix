# Roadmap

Updated 2026-11-19. The yardstick is `BIZNES_MODEL.md`: does it save teachers time, or build
parents' trust? Work that does neither waits.

## In production now

- Photo → OCR → one AI call that grades and checks originality, in three modes chosen from the
  subject (problem solving, language, written response). Teacher rubrics override the defaults.
- Teacher review queue: flagged work first, then oldest; one click confirms all clean work of an
  assignment; the grading screen shows the student's photo next to the analysis; the 5-point grade
  is suggested from the score (86/71/56 boundaries).
- Notifications that actually fire: new assignment, confirmed grade (student and parents),
  next-day deadline reminder (16:00 Tashkent), AI budget at 80% (admins).
- Bulk exams, AI budget with degradation (never blocks a submission), Excel import, quarterly PDF
  reports, XP/streaks/badges, AI tutor with an answer-leak guard, Telegram bot.
- Optional paid module: psychological well-being (off by default).
- Postgres RLS on tenant tables, outbox-based RabbitMQ pipeline, CI + auto-deploy with rollback,
  nightly database backups.

## Next — needed to sell

1. **Tekshir-bot lead magnet.** A teacher sends a photo to the Telegram bot and gets a checked page
   back without an account. The cheapest way to show the product to a director.
2. **Plan limits and subscriptions.** `subscription_ends_at` and the per-plan student/AI limits are
   not enforced; changing a school's monthly AI allowance needs SQL. Add plan management for the
   platform operator and degrade (never block) when a plan lapses.
3. **Usage and value report for directors.** Hours saved per teacher, AI checks used, share of AI
   suggestions the teachers kept — the KPI the paid pilot is judged on.
4. **Grade export to Kundalik.** Directors won't run two journals.
5. **Parent Premium** (15 000 so'm/month) once Telegram linking passes 60% of parents.

## Next — product quality

- Teacher-facing mode override per assignment (e.g. a literature essay graded as "language").
- Per-step accept/reject so teacher corrections can train the rubric defaults.
- Student "fix and resubmit" for language work, with the corrections shown inline.
- Admin view of AI cost per class and subject (the data is already logged in `ai_usage_logs`).

## Known gaps

- Frontend types are hand-written; an OpenAPI-generated client would remove drift.
- 26 of 36 tenant tables are on the RLS backlog (`TenantRlsCoverageTest` lists them with reasons).
- The legacy `domain/`, `infrastructure/`, `interfaces/web/` tree still holds most controllers and
  entities; migrate a context only when a feature needs it.
- Talent analysis and login-time behaviour analysis are not built (need a login history table).
- No sales contact on the landing page yet.
