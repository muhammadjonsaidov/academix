-- academix_backend_tdd.md §4.1 table 18 / academix_tz.md §1.23 — real spec table. One of the
-- 9 RLS-enabled tables (backend_tdd.md §4.1.1 / CLAUDE.md architecture section).
-- No plagiarism fields at all (exams are proctored on paper, no plagiarism check) — deliberately
-- absent, not just null, unlike ai_feedbacks.plagiarism_type/plagiarism_score.
CREATE TABLE exam_ai_feedbacks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE, -- RLS uchun, exam_submissions dan denormalized
    exam_submission_id UUID UNIQUE NOT NULL REFERENCES exam_submissions(id) ON DELETE CASCADE,
    extracted_text TEXT,
    step_analyses JSONB,
    criteria_scores JSONB,
    ai_score_percent REAL,
    feedback TEXT,
    handwriting_match_score REAL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE exam_ai_feedbacks ENABLE ROW LEVEL SECURITY;
CREATE POLICY school_isolation ON exam_ai_feedbacks
  USING (school_id = current_setting('app.current_school_id')::uuid);
