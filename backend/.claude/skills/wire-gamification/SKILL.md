---
name: wire-gamification
description: Implement or change XP, streak or badge logic (progress context, XPService) with AcademiX's anti-gaming rules. Trigger for "XP", "streak", "badge", "gamification", "award points".
---

# AcademiX gamification

The rules exist so students can't farm rewards with empty or low-effort submissions. Change them only as a product decision, and update `academix_tz.md` §4 (XPService) when you do.

1. XP and streak fire only when work reaches `AI_DONE` or `GRADED`, never on `SUBMITTED`.
2. A streak needs `finalScorePercent >= 30%`.
3. Late work earns 50% of the tier XP (halve the tier, not a fixed deduction).
4. Teacher "excellent" adds a flat +20 XP on top.
5. Awards are a side effect of grading (`ProgressAchievementAwarder` / `XpStudentAchievementUpdater` ports into `progress`), not something an endpoint triggers directly; make them idempotent per submission so a redelivered grading event can't double-award.
6. Badge thresholds come from the badge catalog (`BadgeSeedRunner`) — add new badges there.

Cover changes with `XPServiceTest`-style unit tests (pure math) plus an integration test if persistence changes.
