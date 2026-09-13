# Railway deployment — AcademiX AI

Six services from this monorepo, one Railway project. The telegram-bot is deliberately **not**
deployed (it runs on a local PC — see "Local telegram-bot" below). ClamAV is not deployed
(off by default; add later on Hobby if needed). Nginx is not needed — Railway terminates TLS
and routes per-domain.

| Railway service name | Root Directory | Volume mount | Public? |
|---|---|---|---|
| `postgres` | `infra/railway/postgres` | `/var/lib/postgresql` | No (TCP proxy only for local bot) |
| `redis` | `infra/railway/redis` | `/data` | No (TCP proxy only for local bot) |
| `rabbitmq` | `infra/railway/rabbitmq` | `/var/lib/rabbitmq` | No (TCP proxy only for local bot) |
| `seaweedfs` | `infra/railway/seaweedfs` | `/data` | No |
| `backend` | `backend` | — | Yes → `api.academixai.uz` |
| `frontend` | `frontend` | — | Yes → `www.academixai.uz` |

**Name the services exactly as above** — the env vars below reference private-network hostnames
like `postgres.railway.internal`, which are derived from the service name.

Each service picks up its `railway.toml` automatically from its Root Directory. The
`watchPatterns` in those files mean a commit only rebuilds the services whose directory it
touched.

## Setup order

1. **Connect GitHub** to the Railway project (trial: this lifts the "network restrictions").
2. Create the four infra services first (postgres, redis, rabbitmq, seaweedfs): New Service →
   GitHub Repo → set Root Directory → attach volume at the mount path above → set env vars
   (below) → deploy.
3. Create `backend`, then `frontend` (frontend last — its build needs the backend URL decided).
4. Generate a domain (or attach the custom domains) on backend + frontend only.

## Environment variables

Generate three strong secrets first (e.g. `openssl rand -hex 32` each): `JWT_SECRET`,
plus passwords below. Use a Railway **shared variable** for `JWT_SECRET` (backend + frontend
must be byte-for-byte identical — a mismatch silently redirects every request to /login).

### postgres
```
POSTGRES_USER=academix
POSTGRES_PASSWORD=<strong secret>
POSTGRES_DB=academix
ACADEMIX_APP_PASSWORD=<strong secret>      # the restricted NOBYPASSRLS runtime role
```

### redis
```
REDIS_PASSWORD=<strong secret>
```

### rabbitmq
```
RABBITMQ_DEFAULT_USER=academix
RABBITMQ_DEFAULT_PASS=<strong secret>
```
(Required: the built-in guest/guest only accepts loopback connections — over Railway private
networking the backend cannot authenticate as guest at all.)

### seaweedfs
```
SEAWEEDFS_ACCESS_KEY=<strong secret>
SEAWEEDFS_SECRET_KEY=<strong secret>
```

### backend
```
SPRING_PROFILES_ACTIVE=prod                # 'local' would run the dev seed CommandLineRunner
SERVER_PORT=8080

DATABASE_URL=jdbc:postgresql://postgres.railway.internal:5432/academix
DATABASE_USERNAME=academix                 # superuser — Flyway only (CREATE EXTENSION)
DATABASE_PASSWORD=<same as POSTGRES_PASSWORD>
DATABASE_APP_USERNAME=academix_app         # restricted role — actual runtime datasource
DATABASE_APP_PASSWORD=<same as ACADEMIX_APP_PASSWORD>
DB_POOL_SIZE=5                             # postgres is tuned to max_connections=30

REDIS_HOST=redis.railway.internal
REDIS_PORT=6379
REDIS_PASSWORD=<same as redis service>

RABBITMQ_HOST=rabbitmq.railway.internal
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=academix
RABBITMQ_PASSWORD=<same as RABBITMQ_DEFAULT_PASS>

SEAWEEDFS_S3_ENDPOINT=http://seaweedfs.railway.internal:8333
SEAWEEDFS_ACCESS_KEY=<same as seaweedfs service>
SEAWEEDFS_SECRET_KEY=<same as seaweedfs service>
SEAWEEDFS_BUCKET=homeworks

JWT_SECRET=<shared variable>
FRONTEND_URL=https://www.academixai.uz     # CORS allowlist — exact origin, no trailing slash
COOKIE_DOMAIN=.academixai.uz               # see AuthController; leave UNSET while testing on
                                           # *.railway.app domains (cross-site there anyway)

AI_API_KEY=<real key>                      # empty = graceful degradation to AI_SKIPPED
AI_BASE_URL=https://api.deepseek.com       # or another OpenAI-compatible endpoint
AI_MODEL_TEXT=deepseek-v4-flash
GOOGLE_VISION_API_KEY=<real key>
GMAIL_USERNAME=<gmail address>             # password-reset email; optional for demo
GMAIL_APP_PASSWORD=<gmail app password>
TELEGRAM_BOT_USERNAME=<bot username>       # deep-link URLs only

SERVER_TOMCAT_THREADS_MAX=20               # optional: fewer thread stacks under the 0.5GB cap
```
JVM tuning is baked into `backend/Dockerfile` (`JAVA_TOOL_OPTIONS`: 224m heap, SerialGC —
RSS ~380-450MB, fits the trial's 0.5GB/service cap). Override by setting `JAVA_TOOL_OPTIONS`
as a service variable if it OOMs (Railway metrics will show it) — first thing to try is
lowering `-Xmx` to 192m, not raising it.

If the backend can't reach `*.railway.internal` hosts at boot: Railway private networking is
IPv6-only. Append `-Djava.net.preferIPv6Addresses=true` to `JAVA_TOOL_OPTIONS`.

### frontend
```
NEXT_PUBLIC_API_URL=https://api.academixai.uz/api/v1   # BUILD-time: inlined by next build
JWT_SECRET=<shared variable>                           # runtime: proxy.ts route-guard
```
`NEXT_PUBLIC_*` changes require a **redeploy (rebuild)**, not a restart.

## Custom domain (academixai.uz)

1. Railway → `frontend` service → Settings → Networking → Custom Domain → `www.academixai.uz`.
   Railway shows a CNAME target — add at your DNS provider: `CNAME www → <target>`.
2. Same on `backend` for `api.academixai.uz`: `CNAME api → <target>`.
3. Optionally redirect the bare apex `academixai.uz` → `www` at your registrar (most .uz DNS
   panels have a redirect option; Railway needs the CNAME-capable subdomains).
4. Set `COOKIE_DOMAIN=.academixai.uz` on the backend and redeploy. Without it, proxy.ts on
   `www` never sees the auth cookie and every dashboard navigation bounces to /login.
5. Frontend was built with `NEXT_PUBLIC_API_URL` pointing at `api.academixai.uz` already? If
   you first deployed against `*.railway.app` URLs, rebuild the frontend after changing it.

**Trial note:** if Railway refuses to attach a custom domain on the trial plan, demo on the
generated `*.railway.app` domains. Everything works except cookie-dependent flows (F5 session
restore, cookie-fallback refresh) — `railway.app` is on the Public Suffix List, so the two
services are different *sites* and the cookies can't cross. Login itself works (token lives in
Zustand memory). Don't debug it — it fixes itself the moment the custom domains attach.

## Seeding a first admin

`SPRING_PROFILES_ACTIVE=prod` skips dev seeding, so a fresh DB has no users. Easiest path:
Railway `postgres` service → Data/Shell → run `infra/seed/demo-seed.sql` (psql). Remember the
Sprint-1 lesson: an admin without a `schools` row pointing at them (`schools.admin_id`) gets
`schoolId: null` and 500s on school-scoped writes — the seed file handles this; ad-hoc admin
inserts must too.

## Local telegram-bot → Railway infra

The bot needs Postgres, Redis and RabbitMQ but runs on your PC, outside Railway's private
network. Enable **TCP Proxy** (service → Settings → Networking → TCP Proxy) on `postgres`
(port 5432), `redis` (6379) and `rabbitmq` (5672). Railway gives you a public
`host:port` pair for each; put those in `telegram-bot/.env`:

```
DATABASE_URL=jdbc:postgresql://<pg-proxy-host>:<pg-proxy-port>/academix
DATABASE_APP_USERNAME=academix_app
DATABASE_APP_PASSWORD=<ACADEMIX_APP_PASSWORD>
REDIS_HOST=<redis-proxy-host>  REDIS_PORT=<redis-proxy-port>  REDIS_PASSWORD=...
RABBITMQ_HOST=<rmq-proxy-host> RABBITMQ_PORT=<rmq-proxy-port>
RABBITMQ_USERNAME=academix     RABBITMQ_PASSWORD=...
TELEGRAM_BOT_TOKEN=<same token as nothing on Railway — only your PC has it now>
```
TCP proxies are public internet endpoints — they're only as safe as the passwords. That's why
every datastore above requires one. Turn the proxies off when the bot isn't running.

## Cost control (trial: $5 total)

- App Sleeping is enabled in `backend/railway.toml` + `frontend/railway.toml` only. The
  frontend reliably sleeps; the backend may be kept awake by AMQP heartbeats — check the
  metrics panel after a quiet hour. Expected burn: ~$0.30-0.40/day → roughly 13-15 days.
- The four infra services must never sleep (`sleepApplication = false` is explicit).
- When you're done testing for the day, you can also manually **Remove** the backend deploy
  (Deployments → ⋮ → Remove) and redeploy next session — volumes and config survive.
- Upgrading to Hobby later changes nothing in this setup; the same services keep running.

## Verification checklist (first deploy)

1. `postgres` logs: role `academix_app` created (init script ran on the empty volume).
2. `backend` boot: Flyway applies all migrations, `/actuator/health` → 200.
3. Real browser (not curl — CORS is invisible to curl): login at the frontend URL works.
4. Submit a homework with a photo → backend logs show the afterCommit queue publish →
   consumer picks it up (RLS `SET LOCAL` from message `schoolId`) → status reaches
   `AI_DONE`/`AI_SKIPPED` (SKIPPED is correct if no AI_API_KEY is set).
5. `rabbitmqctl list_queues` in the rabbitmq service shell: queues exist, DLQ empty.
6. On custom domains: F5 on a dashboard stays logged in (cookie domain works).
