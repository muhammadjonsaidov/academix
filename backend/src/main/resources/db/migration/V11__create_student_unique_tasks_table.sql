-- academix_backend_tdd.md §4.1 table 8 / academix_tz.md §1.9
-- No school_id column at all (scoped indirectly via assignment_id -> homework_assignments),
-- so no RLS here — matches the DDL exactly. Schema only for now: the unique-task-generation
-- pipeline that writes to this table is Sprint 3+ scope (see ROADMAP.md); homework_submissions
-- below needs this table to exist for its student_task_id FK regardless.
CREATE TABLE student_unique_tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    assignment_id UUID NOT NULL REFERENCES homework_assignments(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    task_content TEXT NOT NULL,
    teacher_approved BOOLEAN DEFAULT FALSE,
    flagged_for_review BOOLEAN NOT NULL DEFAULT FALSE,
    fallback_to_standard BOOLEAN NOT NULL DEFAULT FALSE,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP
);
