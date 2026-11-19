---
name: wire-ai-integration
description: Add or change an AI call in AcademiX — grading/plagiarism, tutor chat, psychology analysis (optional module), OCR via Google Vision — including prompts, response parsing, budget checks and graceful degradation. Trigger for "call the AI", "change the prompt", "wire up grading/chat", "AI budget check", "OCR".
---

# Wire an AcademiX AI call

## Where things are
- Text model: `infrastructure/ai/OpenAiCompatibleClient` (any OpenAI-compatible endpoint, `AI_BASE_URL`/`AI_MODEL_TEXT`, DeepSeek by default), behind the shared-kernel `shared/ai/AiProvider`. Each context narrows it with its own port (`intelligence…GradingAi`, `wellbeing…BehaviorAnalyzer`).
- OCR: `GoogleVisionClient` → `GoogleVisionOcrGateway` (`OcrGateway.extract(image) → String`).
- Both are wrapped by Resilience4j circuit breakers (`aiProvider`, `googleVision`); fallbacks throw typed `…UnavailableException`s that callers turn into degraded results.
- Prompt contracts: `academix_tz.md` §3 — update it when you change a prompt or response schema.

## Rules
1. **Budget first.** `AiBudgetService.isWithinAiBudget(schoolId, AiCallCategory)` before the call, `recordAiUsage` after a successful one. Categories: `EXAM` 15% (protected), `HOMEWORK` 65%, `CHAT` 20%; Redis keys `ai_budget_{category}:{schoolId}:{yyyy-mm}`.
2. **Degrade, never block.** Chat is blocked first; homework falls to `AI_SKIPPED` with OCR text kept (teacher grades manually); exams degrade last. A submission is never rejected because of AI budget or provider failure.
3. **Grading + plagiarism stay one call, in the subject's mode.** `GradingMode.forSubject(SubjectType)` picks PROBLEM_SOLVING / LANGUAGE / WRITTEN_RESPONSE; `OpenAiCompatibleClient.systemPromptFor(mode)` holds one prompt per mode over a single shared JSON contract (so the parser and UI stay shared), and `GradingModes.defaultCriteria(mode)` is the rubric when the teacher set none. Adding a subject type = map it in `forSubject`; adding a mode = prompt + default rubric (sum 100) + UI label in the teacher's submission page + tests (`GradingModesTest`, `OpenAiCompatibleClientTest`).
4. **The model never sets the final score.** It returns `criteriaScores`; the backend computes `aiScorePercent = Σ score·weight / 100`, and the teacher's grade is final.
5. **Tutor chat has two jailbreak layers**: the system prompt and the bare-answer heuristic (`isBlocked=true, blockReason="POTENTIAL_ANSWER_LEAK"`). Keep both.
6. Exams skip plagiarism (proctored on paper).
7. Parse model output defensively (strip markdown fences, unknown enum → safe default) and unit-test the parser with real-shaped JSON — never call the real API in tests.
