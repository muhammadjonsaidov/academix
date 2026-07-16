-- Deviation, judgment call (see ExamGrade.java): not in any spec doc — grades.submission_id FK
-- is scoped to homework_submissions only, so exam grading needs its own table. Mirrors grades
-- exactly (no school_id/no RLS — access controlled via the exam_submissions RLS join), FK'd to
-- exam_submissions instead.
CREATE TABLE exam_grades (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    exam_submission_id UUID UNIQUE NOT NULL REFERENCES exam_submissions(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    score INT NOT NULL CHECK (score BETWEEN 0 AND 100),
    five_point_grade INT NOT NULL CHECK (five_point_grade BETWEEN 2 AND 5),
    teacher_comment TEXT,
    graded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
