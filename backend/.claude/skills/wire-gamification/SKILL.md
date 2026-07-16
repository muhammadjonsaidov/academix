---
name: wire-gamification
description: Implement or change XP, streak, or badge logic (XPService) following AcademiX's deliberate anti-gaming rules. Use whenever touching XP awards, streak tracking, or badge unlocks. Trigger for "XP", "streak", "badge", "gamification", "award points".
---

# Wire AcademiX gamification logic

CLAUDE.md calls this out explicitly as deliberate anti-gaming design — the rules exist specifically to prevent students from gaming the system by submitting empty/low-effort work fast, so don't "simplify" them.

## Non-negotiable rules

1. **XP and streak only fire on `AI_DONE` or `GRADED`, never on `SUBMITTED`.** If you're writing code that awards XP at submission time (optimistically, before grading completes), that's the exact anti-pattern this system is designed to prevent — a student could spam submissions for XP without ever actually being graded.
2. **Streak requires `finalScorePercent >= 30%`.** A submission that's graded but scores below that threshold does not extend or start a streak — don't award streak credit purely for "submitted and graded," the score floor is load-bearing.
3. **Late submissions get 50% of the tier XP**, not a flat penalty subtracted from full XP. Compute the tier XP first, then halve it for late submissions — don't apply a fixed point deduction.
4. **Teacher "excellent" override adds a flat +20 XP bonus on top of the tier XP** — this is additive to whatever the tier/late calculation already produced, not a replacement value.
5. Badge unlock thresholds: check `academix_tz.md` for the exact XP/streak thresholds per badge rather than inventing round numbers — if a badge's unlock condition isn't specified, flag it to the user instead of guessing.

## Where this logic lives

XP/streak changes are a side effect of the grading pipeline completing (queue consumer or grading endpoint), not something the frontend or a standalone endpoint should be able to trigger directly. If you find yourself adding an endpoint that awards XP outside of a grading completion path, stop — that's new surface, confirm with the user first.

## After writing

Run `spec-compliance-reviewer` (or the root `business-rules-auditor` agent, which specifically checks this rule set) before considering XP/streak changes done — timing bugs here are easy to write and easy to miss in a quick read-through.
