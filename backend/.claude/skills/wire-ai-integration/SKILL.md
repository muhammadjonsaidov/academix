---
name: wire-ai-integration
description: Wire a new or changed call to Qwen (grading/plagiarism/chat/psychology/generation) or Google Cloud Vision (OCR), following the exact prompt contracts in academix_tz.md §3 and the AI cost/budget system in §8. Use whenever adding an AI-triggering code path, changing a Qwen prompt, or touching AIAnalysisService. Trigger for "call the AI", "add a Qwen prompt", "wire up grading/plagiarism/chat", "AI budget check".
---

# Wire an AcademiX AI integration call

## Contract source

Read `academix_tz.md` §3 for the exact request/response JSON shape before writing a prompt or parsing a response — system prompt wording, field names in the response (`criteriaScores`, `stepAnalyses`, `plagiarismType` enum values `CLEAN|AI_GENERATED|HANDWRITING_MISMATCH|SUSPICIOUS`), and which model (`qwen3.7-max` text-only vs `qwen3.7-plus` multimodal, currently unused) applies.

## Non-negotiable rules from the spec

1. **Budget check first, always.** Every AI call goes through `AIAnalysisService.isWithinAiBudget(schoolId, category)` before hitting Qwen/Vision. Category is one of `EXAM` (15% of `monthlyAiCallLimit`, protected reserve), `HOMEWORK` (65%), `CHAT` (20%, degrades first). Redis keys: `ai_budget_{category}:{schoolId}:{yyyy-mm}` — not Postgres, to avoid row-lock contention under chat traffic.
2. **Never hard-block on budget exhaustion.** Degradation order: chat blocked first → homework grading falls to `AI_SKIPPED` (OCR still runs, teacher grades manually, submission still accepted) → exam budget degrades last (protected by its own reserve). If you're about to write code that rejects a student's submission because the budget ran out, that's wrong — it should still be accepted with `status=AI_SKIPPED`.
3. **Grading + plagiarism are ONE Qwen call**, not two — don't split them back out for "clarity"; that's a deliberate cost optimization (3 external calls → 2: OCR + merged Qwen call).
4. **The AI never emits a final score.** It returns `criteriaScores` (per-criterion, 0-100). The backend computes `aiScorePercent = sum(score * weightPercent) / 100` — don't let a prompt ask the model for a final percentage, and don't trust one if it appears in a response.
5. **Unique-task generation verification is two independent layers**, not one: (a) an optional pluggable deterministic pre-check per `SubjectType` if registered, then (b) a separate, context-free Qwen call that verifies solvability — this must be a genuinely independent call, not a self-check appended to the same conversation/context (the whole point is avoiding correlated failure). Max 2 regeneration attempts, then `fallbackToStandard=true` + `flaggedForReview=true` for that one student only.
6. **AI Tutor chat jailbreak defense is two layers**: system prompt instructs never to give final answers, AND a response-level heuristic — if the response is suspiciously short/a bare number or formula with no step explanation, override with `isBlocked=true, blockReason="POTENTIAL_ANSWER_LEAK"` regardless of what the model actually returned. Don't implement only the system-prompt layer and skip the heuristic override, or vice versa.
7. **Exams skip plagiarism checks** (proctored on paper) but keep handwriting checks — don't copy the homework pipeline's plagiarism logic into the exam pipeline.
8. **Circuit breaker**: Resilience4j on Qwen calls — 50% failure rate or 75% slow-call rate (>10s) over a 10-call window trips it, 60s open state, fallback response. Don't call Qwen directly without going through whatever wraps it with this breaker.

## After writing

If you changed a prompt's wording or response schema, note that `academix_tz.md` §3 may need a human-approved update to stay accurate — don't edit the spec doc yourself (the `spec-guard` hook will ask for confirmation if you try).
