---
name: wire-notification
description: Implement or change notifications in AcademiX — in-app inbox, preferences, Telegram delivery (separate telegram-bot service), and the psychology add-on's severity→notify matrix. Trigger for "notification", "notify parent/teacher", "telegram", "psych signal", "severity matrix".
---

# AcademiX notifications

## Delivery
- `notification/application/NotificationService` creates in-app notifications, checks the user's preferences first, and queues Telegram delivery through the outbox. `HANDWRITING_PROFILE_RESET` is retired (kept only so old rows load).
- Telegram linking: `POST /api/v1/notifications/telegram/link-token` → single-use Redis token (5 min TTL, 5/hour/user) → the user sends `/start <token>` to the bot.
- The **`telegram-bot/`** service is the bot: long-polling (`getUpdates`, no webhook), role-aware commands (`BotCommandService`), and the consumer of the delivery queue. It shares Postgres/Redis/RabbitMQ and the same `TELEGRAM_BOT_TOKEN`. Mark a notification delivered only after Telegram confirms the send.

## Psychology add-on (only when `ACADEMIX_FEATURE_PSYCHOLOGY=true`)
Severity → recipients is a strict lookup:

| Severity | Class teacher | Psychologist | Parent |
|---|---|---|---|
| LOW | — | — | — (log only) |
| MEDIUM / HIGH | ✓ | ✓ | — |
| CRITICAL | ✓ | ✓ | ✓ |

Parents are never notified below CRITICAL; `isManipulation=true` only adds a flag. `PSYCHOLOGICAL_ALERT` preferences are locked (can't be turned off). With the module off, the bot answers psychologist commands with a "module disabled" message.
