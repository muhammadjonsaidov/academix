-- academix_backend_tdd.md §4.1 (table 19) / academix_tz.md §1.24
-- NOTE: same deliberate RLS exception as school_classes/subjects/student_profiles — not in the
-- 9-table RLS list.
CREATE TABLE import_column_mappings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID UNIQUE NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    mapping JSONB NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
