-- academix_tz.md §2.4 "GET /student/xp-history" — {date, xp, reason} log, missing from the
-- 19-table DDL (see V20's comment / ROADMAP.md Sprint 4). No school_id — scoped by student_id
-- ownership at the query layer, matching student_unique_tasks' precedent.
CREATE TABLE xp_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    xp INT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_xp_history_student ON xp_history(student_id, occurred_at DESC);
