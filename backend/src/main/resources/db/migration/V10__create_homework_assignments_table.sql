-- academix_backend_tdd.md §4.1 table 7 / academix_tz.md §1.8
CREATE TABLE homework_assignments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES school_classes(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(20) DEFAULT 'STANDARD' CHECK (type IN ('STANDARD', 'UNIQUE_GENERATED')),
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deadline_at TIMESTAMP NOT NULL,
    max_score INT DEFAULT 100,
    is_active BOOLEAN DEFAULT TRUE,
    syllabus_reference VARCHAR(255),
    ai_generation_prompt TEXT
);

-- academix_backend_tdd.md §4.1.1 — one of the 9 RLS-enabled tables.
ALTER TABLE homework_assignments ENABLE ROW LEVEL SECURITY;
CREATE POLICY school_isolation ON homework_assignments
  USING (school_id = current_setting('app.current_school_id')::uuid);

-- academix_tz.md §6 — multi-tenancy RLS policy performance.
CREATE INDEX idx_assignments_school ON homework_assignments(school_id);
