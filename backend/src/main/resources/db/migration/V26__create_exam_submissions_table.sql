-- academix_backend_tdd.md §4.1 table 17 / academix_tz.md §1.22 — real spec table. One of the
-- 9 RLS-enabled tables (backend_tdd.md §4.1.1 / CLAUDE.md architecture section).
CREATE TABLE exam_submissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE, -- RLS uchun, exams dan denormalized
    exam_id UUID NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    image_url VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AI_PROCESSING' CHECK (status IN ('AI_PROCESSING', 'AI_DONE', 'AI_SKIPPED', 'GRADED')),
    flagged_for_review BOOLEAN NOT NULL DEFAULT FALSE, -- handwriting match past yoki OCR sifati past
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE exam_submissions ENABLE ROW LEVEL SECURITY;
CREATE POLICY school_isolation ON exam_submissions
  USING (school_id = current_setting('app.current_school_id')::uuid);

CREATE INDEX idx_exam_submissions_exam ON exam_submissions(exam_id, status);
