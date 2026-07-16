-- academix_tz.md §2.3's "POST /teacher/homework/{assignmentId}/submit" ("barcha tasklar
-- tasdiqlangandan keyin o'quvchilarga yuboradi") implies UNIQUE_GENERATED assignments aren't
-- visible to students until the teacher has reviewed/approved every generated task and
-- explicitly published them — homework_assignments has no field for this state. New column,
-- judgment call (see ROADMAP.md Sprint 3). Default TRUE preserves existing behavior for STANDARD
-- assignments (visible immediately, no review step) and every already-created row.
ALTER TABLE homework_assignments ADD COLUMN tasks_published BOOLEAN NOT NULL DEFAULT TRUE;
