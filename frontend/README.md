# AcademiX AI — frontend

Next.js 16 (App Router) + React 19 + TypeScript + Tailwind CSS 4 + shadcn/ui (Base UI) + Zustand.

```
npm install
cp .env.local.example .env.local   # JWT_SECRET must match the backend's byte-for-byte
npm run dev                        # http://localhost:3000
npm run lint
npm run test                       # Vitest
npm run e2e                        # Playwright (needs the app running)
```

- Structure and conventions: [`ARCHITECTURE.md`](ARCHITECTURE.md)
- `proxy.ts` is the route guard (Next.js 16's rename of `middleware.ts`).
- `NEXT_PUBLIC_*` variables are inlined at build time — rebuild after changing them.
