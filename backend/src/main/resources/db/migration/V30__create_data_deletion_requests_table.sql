-- academix_backend_tdd.md §4.1 table 6 / academix_tz.md §2.7 / §7.6 — real spec table, never
-- built despite being on the 9-table RLS list since Sprint 1. One of the 9 RLS-enabled tables.
CREATE TABLE data_deletion_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    requested_by UUID NOT NULL REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP
);

ALTER TABLE data_deletion_requests ENABLE ROW LEVEL SECURITY;
CREATE POLICY school_isolation ON data_deletion_requests
  USING (school_id = current_setting('app.current_school_id')::uuid);
