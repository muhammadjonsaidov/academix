-- academix_tz.md §1.17 — missing from academix_backend_tdd.md's 19-table DDL, new migration
-- (spec gap, see ROADMAP.md Sprint 3). No school_id field in the spec entity — scoped by
-- teacher_id ownership at the query layer, not RLS.
CREATE TABLE teacher_syllabuses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES school_classes(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(10) NOT NULL CHECK (file_type IN ('PDF', 'DOCX', 'IMAGE')),
    extracted_content TEXT,
    is_processed BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_syllabuses_teacher ON teacher_syllabuses(teacher_id);
CREATE INDEX idx_syllabuses_subject_class ON teacher_syllabuses(subject_id, class_id);
