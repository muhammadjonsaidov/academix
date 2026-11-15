---
name: add-import-wizard-view
description: Work on the admin student bulk-import UI (upload → column mapping → preview → commit) backed by useImportWizardStore and the backend analyze/commit endpoints. Trigger for "import wizard", "bulk import UI", "column mapping screen".
---

# AcademiX import wizard (frontend)

Page: `app/dashboard/admin/students/import/page.tsx`; state: `stores/useImportWizardStore.ts` (keeps `fileToken` between steps); types: `types/import.ts`. Backend counterpart: `bulk-import-wizard`.

Steps:
1. **Upload** — validate type/size client-side, call `analyze`.
2. **Mapping** — show `detectedColumns` against `suggestedMapping`; the admin confirms or corrects before moving on (don't auto-advance — confirming is the point).
3. **Preview** — first 5 rows with the mapping applied.
4. **Commit** — send the approved `columnMapping` + `saveMappingAsTemplate`; show `imported`/`failed` and the per-row `errors` (row, field, value, reason) in a form the admin can act on, not just a toast.

The backend owns mapping logic and validation; the UI presents and confirms.
