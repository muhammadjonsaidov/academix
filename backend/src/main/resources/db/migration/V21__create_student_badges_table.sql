-- academix_tz.md §2.4 "GET /student/badges" — join table, missing from the 19-table DDL (see
-- V20's comment / ROADMAP.md Sprint 4). No school_id — scoped by student_id ownership at the
-- query layer, matching student_unique_tasks' precedent.
CREATE TABLE student_badges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    badge_id UUID NOT NULL REFERENCES badges(id) ON DELETE CASCADE,
    awarded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, badge_id)
);

CREATE INDEX idx_student_badges_student ON student_badges(student_id);
