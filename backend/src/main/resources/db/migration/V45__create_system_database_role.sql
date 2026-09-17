-- The second half of the tenancy model.
--
-- Until now the app had exactly one database role (academix_app, created outside Flyway by
-- infra/postgres-init/01-app-role.sh) with RLS enforced on part of the schema. But several
-- units of work are cross-tenant BY DEFINITION and cannot carry a schoolId:
--
--   * the login lookup — no tenant is known before the credentials are verified, so any
--     school-scoped policy on the tables it reads would make signing in impossible;
--   * the outbox publisher and the scheduled audits/report jobs — they exist precisely to
--     sweep every tenant.
--
-- Without a second, explicitly-entered role those call sites either had to stay outside RLS
-- (which is why 22 tenant tables still have no policy — see TenantRlsCoverageTest's allowlist)
-- or would break. So: a NOLOGIN, BYPASSRLS role that no process can connect as, which
-- academix_app may only enter for a single transaction via TenantScope.runAsSystem.
--
-- This is deliberately created by a migration rather than by the init script: hosted Postgres
-- (and Testcontainers) never run postgres-init, and the role must exist on every environment,
-- including databases that already exist.

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'academix_system') THEN
    CREATE ROLE academix_system WITH NOLOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
  END IF;
END
$$;

-- BYPASSRLS is set separately and idempotently: ALTER ROLE keeps re-running migrations harmless.
ALTER ROLE academix_system BYPASSRLS;

-- Only the application role may enter it. A missing academix_app (Testcontainers, where the
-- container superuser runs the app) is not an error: a superuser can SET ROLE into anything.
DO $$
BEGIN
  IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'academix_app') THEN
    GRANT academix_system TO academix_app;
  END IF;
END
$$;

GRANT USAGE ON SCHEMA public TO academix_system;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO academix_system;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO academix_system;

-- Tables created by later migrations must be reachable by the system role too, otherwise the
-- outbox publisher would start failing the first time a feature adds a table it has to sweep.
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO academix_system;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT USAGE, SELECT ON SEQUENCES TO academix_system;
