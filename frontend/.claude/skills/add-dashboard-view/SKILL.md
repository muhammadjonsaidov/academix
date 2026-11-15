---
name: add-dashboard-view
description: Add or change a page/view in one of AcademiX's role dashboards (admin/teacher/student/parent, psychologist when the add-on is on), following the App Router + Zustand + Axios conventions. Trigger for "add a page", "new dashboard view", "build the X screen", "add a route for teacher/student/parent/admin".
---

# Add an AcademiX dashboard view

1. **Route**: `app/dashboard/{admin|teacher|student|parent|psychologist}/…/page.tsx` — `dashboard` is a literal folder (not a route group), so `proxy.ts` can match `/dashboard/<role>`. Mirror the backend path where it helps (`GET /teacher/students/{id}/progress` → `app/dashboard/teacher/students/[studentId]/progress`). Read `node_modules/next/dist/docs/` for any Next.js 16 API you're unsure of.
2. **Shell + nav**: wrap the page in `DashboardShell` from `@/components/shell/DashboardShell`; add a nav item in `components/shared/nav-config.ts` if it's a top-level destination. One `h1` per page.
3. **State**: extend the role's existing store in `stores/` (state + `fetchX`/`updateX` actions in one interface) before creating a new one. Pages call stores, never `apiClient` directly. Calls go through `lib/api/client.ts` (bearer + 401 refresh-retry).
4. **Types**: hand-written in `types/`, matching the backend response exactly.
5. **States**: loading (Skeleton), error, empty (`EmptyState` with the next action), populated. Redirects/`setState` only inside `useEffect`.
6. **Visibility**: students never see plagiarism scores; parents only their own child; psychologists no lesson content. Bulk "approve all" skips `flaggedForReview` items.
7. **Optional modules**: psychology pages/nav go behind `FEATURES.psychology` from `lib/features.ts` (nav items) and are listed in `PSYCHOLOGY_ROUTE_PREFIXES` (route guard).
8. **Styling**: semantic tokens from `globals.css`; chart/inline colors via `components/shared/chart-style.ts` (`--color-*` vars). Base UI primitives in `components/ui/` (not Radix).
9. No i18n plumbing (single language in v1).

Done = `npm run lint && npx tsc --noEmit && npm run test && npm run build` all clean.
