-- academix_backend_tdd.md §4.1 table 6.1 / academix_tz.md §1.5
CREATE TABLE class_subject_teachers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES school_classes(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    academic_year VARCHAR(15) NOT NULL
);

-- academix_backend_tdd.md §4.1.1 — one of the 9 RLS-enabled tables.
ALTER TABLE class_subject_teachers ENABLE ROW LEVEL SECURITY;
CREATE POLICY school_isolation ON class_subject_teachers
  USING (school_id = current_setting('app.current_school_id')::uuid);
