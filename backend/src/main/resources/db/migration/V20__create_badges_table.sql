-- academix_tz.md §2.4/§4 references badges ("GET /student/badges", "checkAndAwardBadges") but
-- defines no catalog anywhere — missing from academix_backend_tdd.md's 19-table DDL entirely.
-- New migration, judgment call (see ROADMAP.md Sprint 4). Catalog rows are seeded via a
-- dev-profile CommandLineRunner, not this migration (seed data isn't schema, matches project
-- convention).
CREATE TABLE badges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    icon VARCHAR(50) NOT NULL,
    criteria_type VARCHAR(20) NOT NULL CHECK (criteria_type IN ('STREAK_DAYS', 'TOTAL_XP')),
    criteria_value INT NOT NULL
);
