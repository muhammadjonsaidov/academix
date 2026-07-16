-- academix_tz.md §1.19 — missing from academix_backend_tdd.md's 19-table DDL, new migration
-- (spec gap, see ROADMAP.md Sprint 3). No school_id field in the spec entity — scoped by
-- teacher_id ownership at the query layer, not RLS.
CREATE TABLE subject_grading_criteria (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    criteria JSONB NOT NULL,
    UNIQUE(subject_id, teacher_id)
);

CREATE INDEX idx_grading_criteria_teacher ON subject_grading_criteria(teacher_id);
