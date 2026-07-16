-- academix_backend_tdd.md §4.1 table 6 / academix_tz.md §1.7 — real spec table, never built
-- despite being referenced by SchoolContextResolver's PARENT-unresolved comment since Sprint 1.
-- NOT RLS-enabled — no school_id column; scope via joined student_profiles.school_id, same
-- pattern as grades/psychological_signals.
CREATE TABLE parent_student_links (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parent_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    student_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    relation VARCHAR(20) NOT NULL CHECK (relation IN ('MOTHER', 'FATHER', 'GUARDIAN')),
    is_active BOOLEAN DEFAULT TRUE,
    biometric_consent_given BOOLEAN NOT NULL DEFAULT FALSE,
    consent_given_at TIMESTAMP,
    UNIQUE(parent_user_id, student_user_id)
);

CREATE INDEX idx_parent_links_student ON parent_student_links(student_user_id);
