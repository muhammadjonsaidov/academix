-- academix_backend_tdd.md §4.1
-- NOTE: same deliberate RLS exception as school_classes above — not in the 9-table list.
CREATE TABLE subjects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    icon VARCHAR(50)
);
