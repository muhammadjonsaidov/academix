-- academix_backend_tdd.md §4.1 table 9 / academix_tz.md §1.10
CREATE TABLE homework_submissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    assignment_id UUID NOT NULL REFERENCES homework_assignments(id) ON DELETE CASCADE,
    student_task_id UUID REFERENCES student_unique_tasks(id) ON DELETE SET NULL,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(15) NOT NULL CHECK (type IN ('TEXT', 'IMAGE', 'MIXED')),
    text_content TEXT,
    image_url VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED', 'AI_PROCESSING', 'AI_DONE', 'AI_SKIPPED', 'GRADED')),
    is_late BOOLEAN DEFAULT FALSE,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    xp_earned INT DEFAULT 0
);

-- academix_backend_tdd.md §4.1.1 — one of the 9 RLS-enabled tables.
ALTER TABLE homework_submissions ENABLE ROW LEVEL SECURITY;
CREATE POLICY school_isolation ON homework_submissions
  USING (school_id = current_setting('app.current_school_id')::uuid);

-- academix_tz.md §6.
CREATE INDEX idx_submissions_student ON homework_submissions(student_id, submitted_at);
CREATE INDEX idx_submissions_assignment ON homework_submissions(assignment_id, status);
CREATE INDEX idx_submissions_school ON homework_submissions(school_id);
