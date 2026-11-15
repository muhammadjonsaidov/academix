# Frontend architecture

AcademiX is a Next.js App Router application. Route files are deliberately thin composition
boundaries; product behaviour must live below them so a feature can evolve without turning a
route into a second application.

## Layers

```
app/                 Route entry points, metadata, loading and error boundaries
components/ui/       Unstyled-to-lightly-styled primitives (Button, Card, Badge, Skeleton)
components/shell/    Persistent application frame and responsive navigation
components/shared/   Cross-feature presentation pieces (charts, empty states, form fields, nav model)
components/<role>/   Components only one role's pages use
features/<feature>/  Optional home for a feature that outgrows a single page (view + its parts)
stores/              Zustand server-state stores, one per UI area (auth, teacher, admin, parent, ...)
lib/                 Framework-neutral utilities and the shared HTTP client
types/               API contracts shared by multiple features
```

`components/ui` never imports a store or calls an API. `components/shell` owns session restore,
real-time subscription and navigation only. A feature owns its own loading, error, empty and
success states. Routes compose a feature view and must not contain transport calls.

## Patterns

- **App Shell**: `DashboardShell` is the single session-aware frame. It keeps desktop and mobile
  navigation visually and behaviourally identical.
- **Page-first composition**: a route's `page.tsx` composes the view and talks to its store, never
  to the HTTP client directly. When a feature grows several sub-components of its own (e.g. the
  admin setup checklist), give it a `features/<name>/` folder; don't move pages there just for
  symmetry.
- **Container/presentation split**: views that fetch or mutate data are containers. Cards, rows,
  form fields and state visuals receive props and remain reusable presentation components.
- **State machine UI**: every remote view renders loading, error, empty and populated states.
  Empty states contain the next meaningful action when the user has permission to take it.
- **Design tokens first**: use the semantic tokens in `globals.css` (`background`, `primary`,
  `success`, severity and role accents). The approved paper/ink palette is not duplicated as
  arbitrary hex values in components.

## Accessibility and responsive contract

- Each route owns one descriptive `h1`; shell text is not a page heading.
- The shell provides a skip link, keyboard-dismissible menus and an Escape-dismissible drawer.
- `main` content is width constrained on large screens and uses `min-w-0` to prevent overflow on
  narrow devices.
- Use `next/link` for internal navigation so App Router prefetching and route announcements work.

## Optional modules

The psychologist add-on (psychological monitoring + Psychologist role) is off unless
`NEXT_PUBLIC_FEATURE_PSYCHOLOGY=true`. `lib/features.ts` is the one place that reads it: the nav
model hides the module's items and `proxy.ts` redirects its routes. The backend enforces the same
switch independently, so the frontend flag is UX only.

## Contracts

Do not break URL contracts, API contracts or role access while moving code.
