---
name: create-migration
description: Create a Flyway migration for AcademiX's PostgreSQL schema, following the exact table/column definitions and RLS conventions in academix_backend_tdd.md §4. Use whenever adding/altering a table or column, adding an index, adding a constraint, or any DDL change to this project's database. Trigger for "add a migration", "new table", "add column", "add index", schema changes.
---

# Create an AcademiX Flyway migration

## Before writing anything

1. **Check whether the entity/table already has a full definition in the spec.** Grep `academix_backend_tdd.md` for the table name — most of the 19 core tables have complete `CREATE TABLE` DDL there (schema section, §4). If it's there, use it verbatim — don't redesign column types/names/nullability from scratch.
2. **No fixed Flyway file-naming convention is specified in the docs** (this is a known gap — the backend TDD only mentions "Flyway rollback" in the rollback runbook, nothing about `V{n}__description.sql` naming). If migrations already exist in the repo, follow their naming pattern. If this is the first migration, ask the user to confirm a naming scheme (standard Flyway `V{version}__{description}.sql` is the safe default) rather than silently picking one.
3. **RLS-scoped table?** If the table is one of the 9 in the RLS list (`homework_assignments`, `homework_submissions`, `class_subject_teachers`, `exams`, `exam_submissions`, `exam_ai_feedbacks`, `ai_chat_messages`, `handwriting_reset_logs`, `data_deletion_requests`), the migration must include both `ALTER TABLE ... ENABLE ROW LEVEL SECURITY` and `CREATE POLICY school_isolation ON <table> USING (school_id = current_setting('app.current_school_id')::uuid)` — don't create the table without them. If it's a new tenant-scoped table NOT on that list, flag this to the user explicitly: is it intentionally excluded (like `student_profiles`/`ai_usage_daily`) or does the spec need updating?

## Writing the migration

- **Zero-downtime / expand-contract for anything touching an existing table in a way that could break running app instances**: adding a nullable column, a new table, or a new index is safe as a single migration. Dropping a column, renaming a column, or adding a NOT NULL constraint to an existing populated column needs an expand step (add new/nullable) in one migration, a backfill, then a contract step (drop old/add constraint) in a later migration — don't collapse these into one migration for anything already in production.
- **Required extensions**: `uuid-ossp` for UUID generation, `vector` (pgvector) for embedding columns (`handwriting_profiles.feature_vector`, 128-dim, `ivfflat` index with cosine ops per TZ §1.13).
- **Indexes**: check `academix_backend_tdd.md`'s index section for any explicitly specified index on the table you're touching before inventing your own.
- **Destructive statements** (`DROP TABLE`/`DROP COLUMN`/`TRUNCATE`) will trigger this repo's `destructive-migration-guard` hook automatically — that's expected, not a bug; confirm when prompted only if the drop is actually intended and the contract phase already shipped.

## Rollback

Backend TDD's rollback runbook assumes Flyway rollback is checked as part of incident response — write migrations so a rollback (restore from backup + revert to prior migration version) is plausible; avoid migrations that are irreversible in practice (e.g. a DROP COLUMN with no way to recover the data) without flagging that tradeoff to the user first.

## After writing

Suggest running the `rls-auditor` agent if the migration touches a `school_id`-bearing table, to confirm RLS coverage matches the spec before merging.
