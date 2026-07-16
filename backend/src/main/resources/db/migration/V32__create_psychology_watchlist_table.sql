-- Deviation, judgment call: academix_tz.md §2.6 documents GET/POST/DELETE
-- /psychologist/watchlist[/{studentId}] endpoints and the dashboard's watchlistStudents field, but
-- no entity or DDL exists anywhere for backing storage. Not RLS-enabled — same reasoning as
-- psychological_signals (no school_id column anywhere in this feature's spec'd tables; scoped via
-- joined student_profiles.school_id at the application level).
CREATE TABLE psychology_watchlist (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    added_by UUID NOT NULL REFERENCES users(id) ON DELETE SET NULL,
    reason TEXT,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
