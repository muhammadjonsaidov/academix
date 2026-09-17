# Tenancy

Tenant isolation is enforced by the database, not by application-level `WHERE school_id = ?`
filtering. Every table that holds tenant data is meant to carry a Postgres row-level security
policy, and every unit of work declares which tenant it acts for. This document is the contract;
the tests named at the bottom are what keep it true.

## One port, two scopes

All database work goes through `uz.academixai.shared.tenancy.TenantScope`:

| Scope | Used by | Effect |
|---|---|---|
| `runAsTenant(schoolId, userId, work)` | HTTP requests (via `RlsTransactionFilter`), queue consumers, batch acceptors, the Wellbeing activity lookup | one transaction with `app.current_school_id` + `app.current_user_id`, RLS applies |
| `runAsSystem(work)` | the outbox publisher, the weekly handwriting audit, the pre-authentication school lookup | one transaction entered as the `academix_system` role, RLS bypassed |

Unscoped access is deliberately not offered. A forgotten scope therefore fails loudly at the
database rather than quietly returning another tenant's rows.

`TenantScopeBoundaryTest` scans the sources and fails the build if a raw `SET LOCAL` / `SET ROLE`
appears anywhere outside `uz.academixai.infrastructure.tenancy`. Six duplicated call sites were
collapsed into the adapter; this test is what stops that from happening again.

## Database roles

| Role | Attributes | Purpose |
|---|---|---|
| migration role (`academix` / container superuser) | superuser | Flyway only: `CREATE EXTENSION`, `CREATE ROLE`, policies |
| `academix_app` | `NOSUPERUSER NOBYPASSRLS` | the app's runtime connection. RLS is inert for a superuser, so this is what makes policies real |
| `academix_system` | `NOLOGIN BYPASSRLS` | created by migration V45; reachable only with `SET LOCAL ROLE` for one transaction, for work that is cross-tenant by definition |

`academix_system` exists because two kinds of work genuinely cannot carry a schoolId: the login
lookup (no tenant is known before the credentials are verified) and the system-wide sweeps (the
outbox publisher, the weekly audit) that exist precisely to visit every tenant. Without it those
call sites would either have to stay outside RLS or break.

Production grants the roles through `infra/postgres-init/01-app-role.sh` (app) and V45 (system).
Migration V45 also carries `ALTER DEFAULT PRIVILEGES`, so tables added by later migrations stay
reachable by the system role — the outbox publisher would otherwise start failing the first time a
feature adds a table it has to sweep.

## Postgres detail worth knowing

`SET LOCAL` on a custom setting (`app.current_school_id`) creates a placeholder for the rest of the
session. Afterwards `current_setting('app.current_school_id', true)` returns an **empty string**,
not `NULL` — which is why unscoped queries in this project surface as
`invalid input syntax for type uuid: ""` rather than as a missing-parameter error. Both are loud
failures; neither leaks data. Do not "fix" a missing scope by adding `missing_ok` to a policy.

## Coverage today, and what unlocks the rest

36 tables hold tenant data (a table counts as tenant data when it carries `school_id` or hangs off
something that does — see `TenantRlsCoverageTest`). 10 are policy-protected; 26 are on the
justified backlog inside that test, each with the concrete remaining work.

The two blockers that unlock most of the list:

1. **`school_id` backfill for child tables** (`grades`, `ai_feedbacks`, `exam_grades`,
   `xp_history`, `student_badges`, `handwriting_profiles`, `psychological_signals`,
   `psychology_watchlist`, `parent_student_links`, `student_unique_tasks`, `syllabus_chunks`).
   A policy without the column would hide rows; the column has to exist and be set on every insert
   path first.
2. **The last two unscoped workers.** `BehaviorAnalysisService` writes signals after its scoped
   read has already committed (each student's read and writes must share one scope), and
   `SyllabusIngestionListener` consumes a message that carries no `schoolId`. Both are small,
   bounded changes.

Because the backlog lives in a test rather than in a document, adding a new tenant table without a
policy fails the build until someone either protects it or writes down why they cannot.

## The tests

| Test | Prevents |
|---|---|
| `TenantScopeBoundaryTest` | tenancy logic drifting back into ad-hoc call sites |
| `TenantRlsCoverageTest` | a new tenant table silently shipping without a policy; a stale backlog entry |
| `TenantScopeIntegrationTest` | a scope that does not actually apply, leaks into the next unit of work on a pooled connection, or a "bypass" role that is not one |
| `RlsMechanismTest` | the mechanism itself regressing (it runs as a restricted role, because Testcontainers connects as a superuser and RLS would otherwise be silently inert) |
