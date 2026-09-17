# Frontend architecture

AcademiX is a Next.js App Router application. Route files are deliberately thin composition
boundaries; product behaviour must live below them so a feature can evolve without turning a
route into a second application.

## Layers

```
app/                 Route entry points, metadata, loading and error boundaries
components/ui/       Unstyled-to-lightly-styled primitives (Button, Card, Badge, Skeleton)
components/shell/    Persistent application frame and responsive navigation
components/shared/   Transitional compatibility exports and cross-feature presentation pieces
features/<feature>/  Feature views, state adapters, view models and feature-only components
stores/              Existing server-state adapters; migrate new feature state behind features/
lib/                 Framework-neutral utilities and the shared HTTP client
types/               API contracts shared by multiple features
```

`components/ui` never imports a store or calls an API. `components/shell` owns session restore,
real-time subscription and navigation only. A feature owns its own loading, error, empty and
success states. Routes compose a feature view and must not contain transport calls.

## Patterns

- **App Shell**: `DashboardShell` is the single session-aware frame. It keeps desktop and mobile
  navigation visually and behaviourally identical.
- **Feature-first composition**: new work goes to `features/<name>/...`, with a small route entry
  in `app/`. Existing pages migrate one vertical feature at a time; do not perform a flag-day move.
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

## Migration rule

Do not break URL contracts, API contracts or role access while moving code. The compatibility
export in `components/shared/DashboardShell.tsx` is intentional; update imports only when the
feature being edited is migrated.
