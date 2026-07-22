-- academix_tz.md §2.1 — audit trail for the class-teacher-assisted STUDENT password reset
-- (PUT /teacher/students/{studentId}/reset-password). Not a spec-documented table (password reset
-- itself is a new §2.1 addition, no §1.x entity given) — shaped identically to
-- handwriting_reset_logs (V24), the closest real analog: same teacher-initiated-on-a-student
-- pattern, same audit-only-no-limit design (low stakes — only the class teacher of that student
-- can call this, gated by RLS + @PreAuthorize, so misuse risk is bounded by the same mechanism
-- already trusted for handwriting resets).
CREATE TABLE password_reset_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE SET NULL,
    reset_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE password_reset_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY school_isolation ON password_reset_logs
  USING (school_id = current_setting('app.current_school_id')::uuid);

CREATE INDEX idx_password_reset_logs_student ON password_reset_logs(student_id, reset_at);
