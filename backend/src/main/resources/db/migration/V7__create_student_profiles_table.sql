-- academix_backend_tdd.md §4.1
-- NOTE: same deliberate RLS exception as school_classes/subjects — not in the 9-table list.
CREATE TABLE student_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    class_id UUID REFERENCES school_classes(id) ON DELETE SET NULL,
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    student_number VARCHAR(30),
    birth_date DATE,
    total_xp INT DEFAULT 0,
    current_streak INT DEFAULT 0,
    max_streak INT DEFAULT 0,
    last_submission_date DATE,
    is_active BOOLEAN DEFAULT TRUE
);
CREATE INDEX idx_student_profiles_school ON student_profiles(school_id);
CREATE INDEX idx_student_profiles_class ON student_profiles(class_id);
