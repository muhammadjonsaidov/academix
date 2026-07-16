-- academix_backend_tdd.md §4.1 table 16 / academix_tz.md §1.21 — real spec table. One of the
-- 9 RLS-enabled tables (backend_tdd.md §4.1.1 / CLAUDE.md architecture section).
CREATE TABLE exams (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES school_classes(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    exam_date DATE NOT NULL,
    max_score INT DEFAULT 100,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE exams ENABLE ROW LEVEL SECURITY;
CREATE POLICY school_isolation ON exams
  USING (school_id = current_setting('app.current_school_id')::uuid);
