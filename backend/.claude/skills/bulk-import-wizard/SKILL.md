---
name: bulk-import-wizard
description: Implement AcademiX's 2-phase admin bulk student import (analyze → commit) with Qwen-assisted column-mapping suggestion and per-school saved mapping templates, per academix_tz.md §2.2. Use whenever touching /admin/students/bulk-import/* endpoints or ImportColumnMapping. Trigger for "bulk import", "excel import", "column mapping", "import students".
---

# AcademiX bulk import backend

## Why it's 2 phases, not 1

Every school's existing student-roster spreadsheet has a different column layout — no fixed template is imposed (per TZ §2.2's explicit note). The flow has to let an admin see and correct the mapping before anything is committed to the DB, which is why it's `analyze` (dry-run, no writes) then `commit` (writes), not a single upload-and-import call.

## Contract (TZ §2.2 — read the full JSON shapes before implementing, this summary is a pointer not the spec)

- `POST /admin/students/bulk-import/analyze` — takes the uploaded file, returns a `fileToken` (temp UUID), `detectedColumns` (raw headers from the file), a Qwen-generated `suggestedMapping` (guesses which detected column maps to which of `firstName`/`lastName`/`phone`/`classId`/`birthDate` from header text alone), and `previewRows` (first 5 rows with the suggested mapping applied).
- `POST /admin/students/bulk-import/commit` — takes the `fileToken`, a (possibly admin-corrected) `columnMapping`, and `saveMappingAsTemplate`. Returns `totalRows`/`imported`/`failed` counts plus a per-row `errors` array (`row`, `field`, `value`, `reason` — e.g. `DUPLICATE_PHONE`, `CLASS_NOT_FOUND`).

## Rules that are easy to get wrong

1. **Partial success is the design, not a bug to fix.** One bad row must not reject the whole file — every valid row commits, every invalid row lands in the `errors` array with enough detail (row/field/value/reason) for the admin to fix and re-run just those. Don't wrap the whole commit in one transaction that rolls back on first error.
2. **`suggestedMapping` is Qwen-generated from header text, not authoritative** — it's a starting point the admin can override before commit. The `commit` call must use whatever `columnMapping` it's actually given, not silently re-derive it.
3. **`ImportColumnMapping` is UNIQUE per school** (TZ §1.24) — `saveMappingAsTemplate: true` should upsert the school's one saved mapping, not create duplicates or a growing history.
4. **`fileToken` is temporary** — treat it as short-lived (analyze uploads to a staging area, commit consumes the token); don't design it as a permanent reference.
5. This still goes through the AI budget system (`suggestedMapping` generation is an AI call) — check `wire-ai-integration` for the budget-check rules; if the column-mapping AI call fails or budget is exhausted, the admin should still be able to proceed with manual mapping (graceful degradation, not a hard block on the whole import flow).

## After writing

Pairs with the frontend `add-import-wizard-view` skill for the corresponding multi-step UI.
