-- academix_backend_tdd.md §4.1 table 13 / academix_tz.md §1.14 — real spec table. NOT RLS-enabled
-- (no school_id column) — multi-tenancy for this table is app-level, via joined
-- student_profiles.school_id, same shape as grades/exam_grades.
CREATE TABLE psychological_signals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    description TEXT,
    raw_evidence JSONB,
    is_manipulation BOOLEAN DEFAULT FALSE, -- faqat flag, notify jarayonini bloklamaydi
    notified_class_teacher BOOLEAN DEFAULT FALSE,
    notified_parent BOOLEAN DEFAULT FALSE,
    notified_psychologist BOOLEAN DEFAULT FALSE,
    resolved BOOLEAN DEFAULT FALSE,
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX idx_psych_signals_student ON psychological_signals(student_id, resolved);
