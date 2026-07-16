---
name: add-dashboard-view
description: Scaffold a page/view inside one of AcademiX's 5 role dashboards (admin/teacher/student/parent/psychologist), following the App Router + Zustand + Axios conventions in academix_frontend_tdd.md. Use when adding a new screen, view, or route inside an existing role dashboard. Trigger for "add a page", "new dashboard view", "build the X screen", "add a route for teacher/student/parent/admin/psychologist".
---

# Add an AcademiX dashboard view

One generic skill for all 5 roles, not 5 separate ones — the pattern is identical, only the role prefix and data-visibility rules change.

## Steps

1. **Locate the route.** Frontend TDD structure: `app/(dashboard)/{admin|teacher|student|parent|psychologist}/...`. Confirm the exact sub-path expected by checking `academix_tz.md` §2 for the corresponding API endpoint's path — the frontend route should mirror the role prefix (e.g. a view backed by `GET /api/v1/teacher/students/{studentId}/progress` lives under `app/(dashboard)/teacher/...`).
2. **Reuse or create the Zustand store.** Check if a domain store already exists (`useAuthStore`, `useExamStore`, `useImportWizardStore`, `useConsentStore`, homework store, or others added since). If the view's data fits an existing store's domain, extend it — don't create a redundant parallel store. Each store bundles state + async action methods (`fetchX`/`updateX`/`approveX`/`submitX`) in one interface; follow that shape.
3. **Wire the API call** through the shared Axios `apiClient` (`baseURL` = `NEXT_PUBLIC_API_URL`, `withCredentials: true`) — never a bare `fetch()` or a second Axios instance. The existing interceptors (bearer injection, 401 refresh-and-retry) only cover calls made through this instance.
4. **Respect cross-role visibility rules** — don't build a UI that could accidentally surface data the backend wouldn't (or shouldn't) return for that role:
   - Student views: no plagiarism/handwriting-match scores.
   - Parent views: only their own child, no class averages or other students.
   - Psychologist views: no lesson/homework content, behavioral signals only.
5. **`flaggedForReview` / `fallbackToStandard`** — if the view includes a bulk "approve all" action, it must skip/disable for any item carrying `flaggedForReview=true`; those need individual review UI, not a silent bypass.
6. **AI-processing status** (`AI_PROCESSING` → `AI_DONE`) is refetched via explicit store actions, not pushed — no WebSocket/SSE per the doc. Don't add polling/websocket logic beyond an explicit user-triggered or on-mount refetch unless the user asks for it specifically (that would be new scope beyond the spec).
7. **Route-guard**: `proxy.ts` (Next.js 16 renamed `middleware.ts` → `proxy.ts`, function `middleware()` → `proxy()` — see CLAUDE.md "Reality checks") only handles role-mismatch redirects at `/dashboard/:path*` — it's UX-only. Don't treat it as real authorization; the backend's `@PreAuthorize` is what actually gates the data this view will render.

## i18n

No language/locale handling — v1 is single-language. If you're about to add a `locale` param or i18n library import, stop; that's out of scope (the `i18n-scope-guard` hook will also ask for confirmation).
