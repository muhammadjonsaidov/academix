-- academix_backend_tdd.md §4.1 table 11 / academix_tz.md §1.12
-- No school_id column (scoped indirectly via submission_id -> homework_submissions) — not RLS
-- eligible, same reasoning as ai_feedbacks above.
CREATE TABLE grades (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    submission_id UUID UNIQUE NOT NULL REFERENCES homework_submissions(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    score INT NOT NULL CHECK (score BETWEEN 0 AND 100),
    five_point_grade INT NOT NULL CHECK (five_point_grade BETWEEN 2 AND 5),
    teacher_comment TEXT,
    teacher_overrode_ai BOOLEAN DEFAULT FALSE,
    ai_original_score REAL,
    graded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- academix_tz.md §6.
CREATE INDEX idx_grades_submission ON grades(submission_id);
