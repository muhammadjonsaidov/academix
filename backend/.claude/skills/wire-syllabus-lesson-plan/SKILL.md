---
name: wire-syllabus-lesson-plan
description: Implement teacher syllabus upload (extraction) and AI-generated lesson plan flow (TeacherSyllabus, LessonPlan) per academix_tz.md §2.3. Use whenever touching /teacher/syllabuses or /teacher/lesson-plans endpoints, or the extraction/generation pipeline behind them. Trigger for "syllabus upload", "lesson plan generation", "extract syllabus content".
---

# Wire AcademiX syllabus + lesson plan generation

Two-stage pipeline: syllabus document → extracted text (async), then extracted text + topic → AI-generated lesson plan (on demand, teacher-triggered).

## Stage 1 — syllabus upload & extraction

- `POST /teacher/syllabuses` — multipart upload (`file`, `subjectId`, `classId`, `title`). Accepted `fileType`: `PDF`, `DOCX`, `IMAGE` (per `TeacherSyllabus` entity, TZ §1.17).
- Extraction happens **async, not inline with the upload response** — `isProcessed` starts false, flips true once `extractedContent` is populated. Don't block the upload request on extraction completing; the doc's pattern elsewhere (homework submissions) is upload-then-process-async, follow the same shape here rather than inventing a synchronous version.
- Extraction method depends on file type: `IMAGE`/scanned `PDF` → Google Vision (OCR, same client used for homework); a text-native `PDF`/`DOCX` may not need Vision at all — don't route every file type through the AI vendor pipeline if a direct text-extraction library would do; that only matters for scanned/image content and needless AI calls also cost budget.
- This extraction call, if it goes through Vision, is still an AI-vendor call — check whether it should count against the `HOMEWORK` budget category or needs its own accounting; the spec doesn't explicitly say, so confirm with the user rather than silently picking one (a wrong guess here either starves homework grading or lets syllabus uploads bypass budget tracking entirely).

## Stage 2 — lesson plan generation

- `POST /teacher/lesson-plans/generate` — body: `syllabusId`, `topic`, `lessonDate`, `classId`. Requires the referenced syllabus to have `isProcessed=true` first (don't let generation proceed against a syllabus whose extraction hasn't finished — the AI has nothing to ground the plan in yet).
- Response: `lessonPlanId`, `aiGeneratedPlan` (JSON structure — the doc doesn't pin down its exact internal shape beyond "JSON struktura", so don't invent a rigid schema the AI has to match exactly; keep it reasonably structured but flexible).
- This is a `qwen3.7-max` text-only call — the extracted syllabus text goes in as context, not the original file. Goes through the standard AI-budget check per `wire-ai-integration` (likely `HOMEWORK` category, same open question as above — confirm, don't assume).
- Teacher review: `PUT /teacher/lesson-plans/{planId}` sets `teacherEditedPlan` and `isApproved`. `aiGeneratedPlan` should stay immutable once written — the teacher's edits go in the separate `teacherEditedPlan` field, so the original AI output remains available for comparison/audit, not overwritten in place.

## After writing

If extraction or generation budget-category isn't clearly assignable to `EXAM`/`HOMEWORK`/`CHAT` per the spec, flag it — this is a real gap, not something to silently resolve either direction.
