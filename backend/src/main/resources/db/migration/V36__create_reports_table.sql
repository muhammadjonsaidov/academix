-- academix_tz.md §2.2 POST/GET /admin/reports — no DDL exists anywhere in
-- academix_backend_tdd.md (same class of gap as notifications/psychology_watchlist/
-- telegram_connections/parent_student_links — new table, deviation). Not RLS-enabled,
-- matching that same precedent: not one of the 9 tables in the backend TDD's RLS list,
-- scoped via service-layer schoolId filtering instead (admin-only resource anyway).
CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('SCHOOL', 'CLASS', 'STUDENT')),
    semester VARCHAR(20) NOT NULL,
    target_id UUID,
    file_url VARCHAR(255) NOT NULL,
    generated_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reports_school ON reports(school_id, generated_at);
