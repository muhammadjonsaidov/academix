-- academix_tz.md §1.18 — missing from academix_backend_tdd.md's 19-table DDL, new migration
-- (spec gap, see ROADMAP.md Sprint 3). No school_id field in the spec entity — scoped by
-- teacher_id ownership at the query layer, not RLS.
CREATE TABLE lesson_plans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES school_classes(id) ON DELETE CASCADE,
    syllabus_id UUID REFERENCES teacher_syllabuses(id) ON DELETE SET NULL,
    topic VARCHAR(255) NOT NULL,
    ai_generated_plan JSONB,
    teacher_edited_plan JSONB,
    is_approved BOOLEAN NOT NULL DEFAULT FALSE,
    lesson_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lesson_plans_teacher ON lesson_plans(teacher_id);
CREATE INDEX idx_lesson_plans_subject_class ON lesson_plans(subject_id, class_id);
