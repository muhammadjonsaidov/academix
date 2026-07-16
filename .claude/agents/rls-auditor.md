---
name: rls-auditor
description: Checks whether every school_id-bearing table has correct Postgres row-level-security coverage, matching the exact 9-table list in academix_backend_tdd.md §4 (homework_assignments, homework_submissions, class_subject_teachers, exams, exam_submissions, exam_ai_feedbacks, ai_chat_messages, handwriting_reset_logs, data_deletion_requests). Use whenever a migration adds/alters a tenant-scoped table, or before any release touching the schema. Narrower and faster than spec-compliance-reviewer — use this for RLS-specific checks, spec-compliance-reviewer for broader contract drift.
tools: Read, Grep, Glob
model: claude-haiku-4-5-20251001
---

CLAUDE.md is explicit: multi-tenancy is enforced via Postgres RLS, not app-level `WHERE school_id = ...` filtering, because a forgotten WHERE clause leaks cross-tenant data. `student_profiles` and `ai_usage_daily` carry `school_id` but are deliberately NOT RLS-enabled per the docs — don't flag those as bugs, but don't treat their pattern as license to skip RLS on tables that should have it either.

## What to check

1. Read `academix_backend_tdd.md`'s schema section for the current canonical RLS table list (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY` + matching `CREATE POLICY school_isolation ...`). Don't trust a cached list — re-grep the doc each run, it's the source of truth.
2. Grep the actual migrations (`*.sql` under any `db/migration`-style path) or JPA entity annotations for every table/entity carrying a `school_id` (or `schoolId`) column.
3. Cross-reference: does every table in the spec's RLS list have both `ENABLE ROW LEVEL SECURITY` and a `CREATE POLICY school_isolation ON <table> USING (school_id = current_setting('app.current_school_id')::uuid)` (or equivalent) in the actual migrations?
4. Does every NEW school_id-bearing table introduced by this change appear in the spec's RLS list? If a new tenant-scoped table isn't on the spec's list, that's not automatically a bug (the spec might just not have been updated) — flag it explicitly as "new tenant table, not in spec's RLS list — confirm intentional before assuming safe to skip."
5. Confirm the `SET LOCAL app.current_school_id = ...` interceptor logic exists somewhere in request-handling code (don't just check schema-level policy, the app has to actually set the session variable per-transaction or the policy is a no-op). Confirm it's inlined as a literal, not a JDBC bind parameter — `SET LOCAL` doesn't accept `?`/`:param` placeholders, that's a real syntax error confirmed by a failing test (see CLAUDE.md "Reality checks").
6. Check the `BYPASSRLS` superadmin role: per spec it should exist but stay commented out/inactive. Flag if it looks active/granted anywhere.
7. **Most important, easiest to silently regress: confirm which role the app's runtime datasource actually connects as.** RLS is completely inert for a superuser or a `BYPASSRLS` role — confirmed by a real test that initially passed when it should have failed. Check `application.yml`'s `spring.datasource.*` uses the restricted `academix_app` role (`infra/postgres-init/01-app-role.sh`), not the migration superuser (`spring.flyway.*` is correctly the superuser — don't flag that half). If anyone ever points `spring.datasource.*` back at the superuser "to fix a permissions error," that's the single most dangerous regression this project can make — flag it as critical, not a style note.

## Output

Per table: table name, RLS status found, matches spec (yes/no/not-in-spec), and the exact fix if missing (the `ALTER TABLE ... ENABLE ROW LEVEL SECURITY` + `CREATE POLICY` statements). If everything checked out, say so plainly.
