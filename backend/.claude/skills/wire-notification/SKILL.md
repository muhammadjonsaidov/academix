---
name: wire-notification
description: Implement or change notification dispatch — the psychological-signal severity→notify matrix, or Telegram delivery. Use whenever touching createSignalAndNotify, Notification entities, or the Telegram bot integration. Trigger for "notification", "psych signal", "severity matrix", "notify parent/teacher/psychologist", "telegram dispatch".
---

# Wire AcademiX notification dispatch

## The severity → notify matrix is a strict lookup table, not a heuristic

| Severity | Class Teacher | Psychologist | Parent |
|---|---|---|---|
| LOW | — | — | — (log/watchlist only, no notify) |
| MEDIUM | ✓ | ✓ | — |
| HIGH | ✓ | ✓ | — |
| CRITICAL | ✓ | ✓ | ✓ |

- Parents are **deliberately never notified below CRITICAL** — the point is an unqualified reading of a raw behavioral signal shouldn't alarm a parent before a psychologist has triaged it. Don't "improve" this by notifying parents earlier even if it seems more transparent — that's the opposite of the documented intent.
- `isManipulation=true` does **not** change notify routing — it only adds a flag visible in the psychologist's view. Don't gate or accelerate notification based on this flag.
- This table governs `PsychologicalSignal` dispatch specifically (`createSignalAndNotify`). Other notification types (`HOMEWORK_GRADED`, `STREAK_MILESTONE`, `BADGE_EARNED`, etc.) aren't severity-gated the same way — don't apply this matrix to unrelated notification types.

## Telegram delivery

- One-time deep-link token flow: `POST /api/v1/notifications/telegram/link-token` generates a Redis-backed token, 5-min TTL, single-use, rate-limited 5/hour/user.
- Webhook (`POST /api/v1/notifications/telegram/webhook`) must validate the `X-Telegram-Bot-Api-Secret-Token` header before processing — mismatch is a 401, since only real Telegram servers can call this endpoint.
- `Notification.sentToTelegram` should only flip true after actual delivery confirmation, not just because a `TelegramConnection` exists and is active — a send failure shouldn't silently mark itself successful.
- `HANDWRITING_PROFILE_RESET` notifications are explicitly meant to reach the parent as a transparency measure (per backend TDD §6.3) — don't accidentally suppress this one under the psych-signal matrix logic above, it's a different notification type with different rules.

## After writing

Run the root `business-rules-auditor` agent (or `spec-compliance-reviewer`) — the severity matrix is exactly the kind of "looks right at a glance, wrong on one row" logic that benefits from an independent check.
