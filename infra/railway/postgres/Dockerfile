# Railway Postgres service. NOT Railway's managed Postgres, deliberately:
#   1. We need pgvector (handwriting biometrics) — vanilla postgres images don't bundle it
#      (confirmed by a real failed migration, see CLAUDE.md "Reality checks").
#   2. We need the academix_app restricted role created at init — RLS is silently inert for
#      the BYPASSRLS superuser, and managed Postgres offers no /docker-entrypoint-initdb.d hook.
#
# Railway service settings: Root Directory = infra/railway/postgres, volume mounted at
# /var/lib/postgresql (pg18 image convention — the PARENT dir, not .../data; the old path
# fails to start, see CLAUDE.md).
#
# Required env: POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DB=academix, ACADEMIX_APP_PASSWORD
FROM pgvector/pgvector:pg18

# Same role-creation script as infra/postgres-init/ (source of truth there — keep in sync).
# Runs only on first boot of an empty data dir.
COPY 01-app-role.sh /docker-entrypoint-initdb.d/01-app-role.sh

# Small-footprint tuning for the Railway trial (0.5GB/service): default shared_buffers is 128MB;
# 32MB is plenty at demo scale. max_connections=30 covers backend Hikari (5) + Flyway + the
# telegram-bot running on a local PC via TCP proxy (5) with headroom.
CMD ["postgres", "-c", "shared_buffers=32MB", "-c", "max_connections=30", "-c", "effective_cache_size=128MB"]
