-- academix_backend_tdd.md §4.1 table 10 / academix_tz.md §1.11
-- No school_id column (scoped indirectly via submission_id -> homework_submissions), so not RLS
-- eligible — not in the 9-table list, and there'd be no column to write a policy against anyway.
CREATE TABLE ai_feedbacks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    submission_id UUID UNIQUE NOT NULL REFERENCES homework_submissions(id) ON DELETE CASCADE,
    extracted_text TEXT,
    ocr_confidence REAL,
    step_analyses JSONB,
    criteria_scores JSONB,
    ai_score_percent REAL,
    feedback TEXT,
    highlighted_errors JSONB,
    plagiarism_score REAL,
    plagiarism_type VARCHAR(30) DEFAULT 'CLEAN',
    handwriting_match_score REAL,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
