---
name: add-endpoint
description: Add a REST endpoint to the AcademiX backend matching the exact contract in academix_tz.md §2 (path, method, request/response shape) plus this project's RLS, auth, and error-code conventions. Use whenever adding, changing, or exposing a new API route/controller action. Trigger for "add an endpoint", "new route", "expose this via REST", "implement this API".
---

# Add an AcademiX REST endpoint

## Before writing anything

1. **Find the exact contract.** Grep `academix_tz.md` §2 for the endpoint — it's organized by role (`2.1 Auth`, `2.2 Admin`, `2.3 Teacher`, `2.4 Student`, `2.5 Parent`, `2.6 Psychologist`, `2.7 Telegram`). Read the full request/response JSON shape as written — field names and casing are exact, not illustrative.
2. **If the endpoint isn't in the spec**, don't invent a shape from scratch silently — this is new surface the spec doesn't cover; confirm the contract with the user before implementing (once confirmed, it's a real gap worth suggesting they add to `academix_tz.md`, via explicit human edit — not something this session edits itself, per the `spec-guard` hook).

## Conventions to follow exactly

- **Path prefix by role**: `/api/v1/{admin|teacher|student|parent|psychologist}/...`, or `/api/v1/auth/...` / `/api/v1/notifications/telegram/...` for the shared ones.
- **RLS**: if the endpoint touches an RLS-covered table (see backend TDD §4's 9-table list), the request-handling path must run inside a transaction that sets `SET LOCAL app.current_school_id = '<uuid-from-jwt>'` before querying — don't rely on an app-level `WHERE school_id = ...` as the only guard.
- **Auth**: `@PreAuthorize` role check matching the path's role segment. Never trust a client-supplied role claim.
- **Cross-role visibility rules** (don't leak fields across roles): student responses never include `plagiarismScore`/`plagiarismType`/`handwritingMatchScore`; parent responses never include other students' data or class-level comparisons; psychologist responses never include lesson/homework content.
- **Error responses**: this project uses a custom 4-column shape (`status`, internal `code` like `ERR_INVALID_FILE`/`ERR_RESET_LIMIT_EXCEEDED`, human `message`, `mitigation`) — see backend TDD's error-code table. This is explicitly NOT RFC 7807 Problem Detail JSON; don't default to Spring's standard error body.
- **AI-triggering endpoints** (submission, exam upload, chat, unique-task generation) must call the budget check (`AIAnalysisService.isWithinAiBudget`) before invoking Qwen/Vision, and degrade gracefully (never hard-block) per TZ §8 — see the `wire-ai-integration` skill for that part specifically.
- **flaggedForReview / fallbackToStandard**: any bulk "approve all" endpoint must exclude items carrying `flaggedForReview=true` server-side, not just rely on the frontend to hide them.

## After writing

Suggest running `spec-compliance-reviewer` against the new endpoint to confirm the path, shape, and error codes match the spec exactly before merging.
