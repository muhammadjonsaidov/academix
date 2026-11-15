---
name: add-endpoint
description: Add or change a REST endpoint in the AcademiX backend — controller, use case, auth, RLS scope, validation, error codes, RBAC test matrix. Use whenever adding, changing or exposing an API route. Trigger for "add an endpoint", "new route", "expose this via REST", "implement this API".
---

# Add an AcademiX REST endpoint

## Shape the contract
- Look for it in `academix_tz.md` §2 (by role: 2.1 Auth … 2.8 Notifications). Use the documented path/JSON shape when it exists. If it doesn't, or the documented shape is wrong for the product, design a sensible one, implement it, and **update §2 in the same change** — the spec is the reference, not a lock.
- Path prefix by role: `/api/v1/{admin|teacher|student|parent|psychologist}/...`; shared ones under `/api/v1/auth`, `/api/v1/notifications`.

## Where the code goes
- Use case in the owning context: `<context>/application/` behind a `port/in` interface; persistence and other contexts through `port/out` + an adapter in `<context>/infrastructure/`. Controller in `<context>/adapter/in/web/` (or `interfaces/web/<role>/` while that context still keeps its controllers in the legacy tree). `ContextBoundaryTest` fails the build if `application` imports infrastructure or another context's infrastructure.
- Request DTOs get bean validation (`@Valid`, Uzbek messages like the existing ones). Errors are `ApiException(status, "ERR_…", message, mitigation)` → the project's 4-field error body, not RFC 7807.

## Rules that bite
- `@PreAuthorize("hasRole('…')")` on the controller; never trust a client-sent role.
- RLS is automatic for authenticated requests (`RlsTransactionFilter` → `TenantScope.runAsTenant`). Don't hand-write `SET LOCAL`.
- The request runs in ONE transaction: a `@Transactional` method that throws `ApiException` needs `noRollbackFor = ApiException.class`; per-item partial success needs `REQUIRES_NEW` on another bean. See `LESSONS.md`.
- Visibility: students never see plagiarism scores; parents only their own child; psychologists no lesson content.
- AI-triggering paths check `AiBudgetService.isWithinAiBudget` and degrade, never hard-fail (see `wire-ai-integration`).
- Bulk "approve all" must skip `flaggedForReview=true` items server-side.
- Optional-module endpoints (psychology) get `@ConditionalOnProperty(prefix = "academix.features.psychology", name = "enabled", havingValue = "true")`.

## Before you're done
- Add the endpoint to `RoleAccessIntegrationTest`'s matrix (the test fails on any unlisted endpoint), with a body that passes validation.
- Mirror the response type in `frontend/types/` by hand (no codegen).
- `./gradlew spotlessApply test` on JDK 21.
