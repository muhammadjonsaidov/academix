#!/usr/bin/env bash
# Runs once at first container init (official Postgres image convention:
# /docker-entrypoint-initdb.d/*.sh). Creates a restricted, non-superuser role for the
# app's actual runtime connection — separate from the POSTGRES_USER superuser
# (which stays reserved for Flyway migrations: CREATE EXTENSION etc. genuinely
# need superuser, ordinary app queries never should).
#
# Why this exists: confirmed by a real failing test that the default POSTGRES_USER
# role is a superuser with BYPASSRLS — meaning every RLS policy in the project would
# be silently inert if the app connected as that role. See CLAUDE.md "Reality checks".
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
  DO \$\$
  BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'academix_app') THEN
      CREATE ROLE academix_app WITH LOGIN PASSWORD '${ACADEMIX_APP_PASSWORD}'
        NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
    END IF;
  END
  \$\$;

  GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO academix_app;
  GRANT USAGE ON SCHEMA public TO academix_app;

  -- Applies automatically to tables Flyway (running as \$POSTGRES_USER) creates later —
  -- solves the ordering problem of granting on tables that don't exist yet at init time.
  ALTER DEFAULT PRIVILEGES FOR ROLE ${POSTGRES_USER} IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO academix_app;
  ALTER DEFAULT PRIVILEGES FOR ROLE ${POSTGRES_USER} IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO academix_app;
EOSQL
