---
name: add-import-wizard-view
description: Build the admin bulk-import UI (upload → column-mapping review → preview → commit) backed by useImportWizardStore, pairing with the backend bulk-import-wizard skill's analyze/commit endpoints. Use whenever touching the student bulk-import screen. Trigger for "import wizard", "bulk import UI", "column mapping screen".
---

# AcademiX import wizard view

## Flow (matches the backend's 2-phase analyze/commit contract)

1. **Upload step** — file picker (client-side validate before upload: reasonable file type/size, this is the same discipline as image uploads elsewhere in the app — JPEG/PNG size checks exist for handwriting photos, apply the equivalent spirit here for spreadsheet files).
2. **Mapping review step** — call `analyze`, render `detectedColumns` against the `suggestedMapping` Qwen guessed, let the admin correct any mapping before proceeding. Don't auto-advance past this step even if the suggestion looks confident — the admin confirming the mapping is the point of this step existing.
3. **Preview step** — show `previewRows` (first 5, mapping applied) so the admin can sanity-check before committing to the full file.
4. **Commit step** — call `commit` with the (possibly corrected) `columnMapping` and `saveMappingAsTemplate` choice. Render the result as partial success: `imported`/`failed` counts plus the per-row `errors` list (row/field/value/reason) in a way the admin can act on — not just a pass/fail toast. If some rows failed, the UI should make it easy to see which and why, not bury it.

## Store shape

Follow the existing `useImportWizardStore` pattern (state + async action methods in one interface, matching `useAuthStore`/`useExamStore`'s shape) — don't introduce a separate state-management approach for this one flow. Store the `fileToken` in the store between the analyze and commit steps, not in component-local state, since the wizard spans multiple steps/components.

## Don't

- Don't let the frontend re-derive or validate the column mapping logic itself — that's the backend's job (and partly Qwen's, for the initial suggestion); the frontend's job is presenting it for confirmation and sending back whatever the admin approved.
- Don't collapse this into a single-step upload — the whole reason it's multi-step is per-school spreadsheet formats vary and need human confirmation before commit.
