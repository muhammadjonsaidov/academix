-- academix_backend_tdd.md §4.1 table 12 / academix_tz.md §1.13 — real spec table, not a gap
-- (unlike Sprint 3/4's new tables); just never built until now. No school_id column in the
-- spec's own DDL — scoped by student_id alone (one profile per student, UNIQUE), not RLS.
CREATE TABLE handwriting_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    samples_count INT DEFAULT 0,
    is_reliable BOOLEAN DEFAULT FALSE,
    feature_vector vector(128),
    profile_version VARCHAR(20) DEFAULT 'v1',
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_reset_by_teacher_id UUID REFERENCES users(id) ON DELETE SET NULL,
    last_reset_reason VARCHAR(30) CHECK (last_reset_reason IN ('ILLNESS','INJURY','TRANSFER_STUDENT','OTHER')),
    last_reset_at TIMESTAMP,
    reset_count_this_semester INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_handwriting_vector ON handwriting_profiles
    USING ivfflat (feature_vector vector_cosine_ops);
