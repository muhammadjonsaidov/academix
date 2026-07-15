-- DEVIATION from academix_tz.md §1.1 / academix_backend_tdd.md §4.1 — the spec's User entity
-- has no schoolId field at all (by design: ADMIN resolves its school via schools.admin_id,
-- STUDENT via student_profiles.school_id). But TEACHER has no equivalent link anywhere in the
-- schema, which makes the documented POST /admin/teachers/invite + GET /admin/teachers contract
-- (academix_tz.md §2.2) impossible to implement correctly: a freshly invited teacher, before any
-- class_subject_teachers assignment exists, would be unscoped to any school and invisible/
-- unauthorizable. Nullable so ADMIN/STUDENT/PARENT/PSYCHOLOGIST rows (scoped elsewhere) are
-- unaffected; only ever populated for TEACHER at invite time. See CLAUDE.md "Reality checks".
ALTER TABLE users ADD COLUMN school_id UUID REFERENCES schools(id) ON DELETE SET NULL;
CREATE INDEX idx_users_school_id ON users(school_id) WHERE school_id IS NOT NULL;
