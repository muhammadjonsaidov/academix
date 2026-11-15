---
name: create-migration
description: Create a Flyway migration for AcademiX's PostgreSQL schema — tables, columns, indexes, constraints, RLS policies, data fixes. Use for any DDL or data-migration change. Trigger for "add a migration", "new table", "add column", "add index", "drop table", schema changes.
---

# Create an AcademiX Flyway migration

- Location/naming: `backend/src/main/resources/db/migration/V{next}__snake_case_description.sql` (check the highest existing `V` number). Never edit an applied migration.
- Flyway runs as the superuser; the app runs as the restricted `academix_app` role, which gets privileges on new tables automatically (`ALTER DEFAULT PRIVILEGES` in `infra/postgres-init/01-app-role.sh`).
- New tenant table: `school_id UUID NOT NULL`, then `ALTER TABLE … ENABLE ROW LEVEL SECURITY` + `CREATE POLICY school_isolation ON … USING (school_id = current_setting('app.current_school_id')::uuid)`. If it can't be protected yet, add a justified entry to `TenantRlsCoverageTest` — the build fails otherwise.
- Extensions available: `uuid-ossp`, `vector` (Postgres image is `pgvector/pgvector:pg18`).
- Production-safe changes: adding nullable columns/tables/indexes is one step. Renames, drops and new NOT NULL on populated columns go expand → backfill → contract across releases.
- Destructive steps (`DROP`, `TRUNCATE`, data rewrites) are allowed when intended — say so in the migration comment, make sure the code no longer needs the data, and tell the user before it ships (production data can't be recovered without the backup). Example of a careful data fix: `V47__retire_unique_tasks_and_syllabus_pipeline.sql`.
- Record schema changes in `academix_backend_tdd.md` §4.
- Verify: the Testcontainers suite applies every migration from scratch (`./gradlew test`), so a broken migration fails CI.
