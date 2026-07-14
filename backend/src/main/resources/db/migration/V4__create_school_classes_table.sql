-- academix_backend_tdd.md §4.1
-- NOTE: carries school_id but is deliberately NOT RLS-enabled — not one of the 9
-- tables in the backend TDD's RLS list (same pattern as student_profiles/ai_usage_daily).
-- See CLAUDE.md "Backend architecture" for why this is an intentional exception.
CREATE TABLE school_classes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    grade INT NOT NULL CHECK (grade BETWEEN 1 AND 11),
    letter VARCHAR(5) NOT NULL,
    full_name VARCHAR(15) NOT NULL,
    class_teacher_id UUID REFERENCES users(id) ON DELETE SET NULL,
    student_count INT DEFAULT 0,
    academic_year VARCHAR(15) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);
