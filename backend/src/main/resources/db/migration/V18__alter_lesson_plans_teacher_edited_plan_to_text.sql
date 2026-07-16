-- Fixes a real bug found in Sprint 3: teacher_edited_plan (TZ §1.18/§2.3's PUT body example is a
-- bare string, e.g. "teacherEditedPlan": "...") is free text, not a JSON document. V16 mapped it
-- as jsonb, which requires the column's raw content to itself be valid JSON — Postgres rejects a
-- plain string like "edited" ("invalid input syntax for type json"), confirmed by a real failing
-- PUT /teacher/lesson-plans/{id} request. ai_generated_plan stays jsonb (that one really is a
-- structured object, see QwenAIClient's LESSON_PLAN_SYSTEM_PROMPT).
ALTER TABLE lesson_plans ALTER COLUMN teacher_edited_plan TYPE TEXT USING teacher_edited_plan::text;
