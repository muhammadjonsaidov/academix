---
name: implement-section
description: Scaffold or extend AcademiX implementation code for one specific section of the spec docs (academix_tz.md, academix_backend_tdd.md, academix_frontend_tdd.md), pulling the exact contract text instead of reconstructing it from memory or a prior summary. Use when the user asks to implement, scaffold, start, or build a specific TZ/TDD section, entity, endpoint group, or subsystem (e.g. "implement TZ §2.3 Teacher API", "build the AI budget system", "scaffold the HandwritingProfile entity", "start on the psychologist dashboard"). Also trigger for "implement this endpoint/entity/table from the spec".
---

# Implement a spec section

AcademiX is spec-only right now (per CLAUDE.md): three long Uzbek-language docs are the source of truth, and no package/module layout has been decided yet. This skill's job is to keep new code anchored to the exact contract text, and to make the layout decision explicit and consistent instead of ad hoc.

## Steps

1. **Resolve the target section.** Parse the user's reference (a section number like "TZ §2.3", a subsystem name like "AI budget system", an entity like "HandwritingProfile"). Grep the three docs for the matching heading/anchor — don't guess which doc holds it from memory:
   - `academix_tz.md` — entities §1, REST API §2, AI prompt contracts §3, service-layer signatures §4, security §5, DB indexes §6, Telegram/handwriting-reset §7, AI budget §8.
   - `academix_backend_tdd.md` — SQL schema + RLS §4, queue topology, env vars, error-code matrix, retention/encryption.
   - `academix_frontend_tdd.md` — App Router layout, Zustand stores, Axios client, middleware route-guard.

2. **Read the full section, not a snippet.** Grep for the heading, then Read enough surrounding lines to get the complete contract — full entity field list, full endpoint request/response shape, full enum, not a truncated match.

3. **Check what already exists.** Look for a backend (`build.gradle`/`src/main/java`) or frontend (`package.json`/`app/`) tree. If neither exists yet, this is the first real code in the repo — before writing anything, use AskUserQuestion to settle the package/module layout (DDD vs layered for backend; confirm the App Router folder conventions already implied by the frontend TDD) since CLAUDE.md flags this as an undecided gap. If code already exists, follow its established layout instead of re-litigating it.

4. **Implement matching the contract exactly**: same enum names and casing, same endpoint paths and HTTP methods, same field names and types, same error-code shape (custom status/internal-code/message/mitigation table, not RFC 7807), same Redis/queue key formats where relevant (e.g. `ai_budget_{category}:{schoolId}:{yyyy-mm}`). Where the spec is genuinely silent on an implementation detail, make the smallest reasonable choice and note it — don't invent naming the spec doesn't imply.

5. **After implementing**, suggest (don't force) running the `spec-compliance-reviewer` agent against the new code before it's considered done — it's built for exactly this drift check.

## What this skill is not

Not a general scaffolding tool — it's specifically about staying faithful to the spec contract. Don't use it for refactors, bug fixes, or code with no corresponding spec section.
