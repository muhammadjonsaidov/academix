---
name: bulk-import-wizard
description: Work on AcademiX's two-phase admin student import (analyze → commit) with suggested column mapping and per-school saved templates (BulkImportService, ImportColumnMapping, /admin/students/bulk-import/*). Trigger for "bulk import", "excel import", "column mapping", "import students".
---

# AcademiX bulk import (backend)

Every school's roster spreadsheet looks different, so the flow is a dry-run `analyze` (no writes) followed by `commit`. Contract: `academix_tz.md` §2.2. Code: `school/application/BulkImportService`, `ColumnMappingSuggester`, Apache POI parsing in `infrastructure/importing/ExcelImportParser`.

- `analyze` → `fileToken` (short-lived staging reference), `detectedColumns`, `suggestedMapping`, first 5 `previewRows`.
- `commit` → uses exactly the `columnMapping` it receives (the admin may have corrected it), returns `totalRows`/`imported`/`failed` + per-row `errors` (`row`, `field`, `value`, `reason`).

Rules:
- **Partial success is the design.** Each row commits in its own transaction (`REQUIRES_NEW` on a separate bean — see `LESSONS.md`); bad rows go to `errors`, they don't roll back the file.
- `ImportColumnMapping` is one row per school: `saveMappingAsTemplate` upserts it.
- `suggestedMapping` is a deterministic synonym heuristic today. Swapping in an AI call is fine, but it must go through the AI budget and fall back to the heuristic/manual mapping if the call fails.

Frontend counterpart: `add-import-wizard-view`.
