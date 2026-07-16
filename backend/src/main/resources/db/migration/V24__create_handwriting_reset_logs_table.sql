-- academix_backend_tdd.md §4.1 table 12.1 / academix_tz.md §1.13.1 — real spec table. One of the
-- 9 RLS-enabled tables (backend_tdd.md §4.1.1 / CLAUDE.md architecture section).
CREATE TABLE handwriting_reset_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE SET NULL,
    reason VARCHAR(30) NOT NULL CHECK (reason IN ('ILLNESS','INJURY','TRANSFER_STUDENT','OTHER')),
    notes TEXT,
    previous_profile_version VARCHAR(20),
    reset_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE handwriting_reset_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY school_isolation ON handwriting_reset_logs
  USING (school_id = current_setting('app.current_school_id')::uuid);

CREATE INDEX idx_reset_logs_student ON handwriting_reset_logs(student_id, reset_at);
