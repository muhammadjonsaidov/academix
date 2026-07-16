-- academix_tz.md §8 "Admin dashboard cost showback" — GET /admin/analytics/ai-usage needs a
-- dimensional (byClass/bySubject/byTeacher) data source that never existed anywhere (AiBudgetService
-- only tracks global Redis counters). No DDL is given for this in any spec doc — new table,
-- deviation, same class of gap as reports/notifications/ai_chat_messages before it. "Faqat grading
-- chaqiruvlaridan" (only from grading calls, TZ §8) — CHAT usage is deliberately never logged here.
-- Not RLS-enabled: not one of the 9 tables in the backend TDD's RLS list, scoped via service-layer
-- schoolId filtering instead (admin-only resource, same precedent as `reports`).
CREATE TABLE ai_usage_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES school_classes(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(20) NOT NULL CHECK (category IN ('HOMEWORK', 'EXAM')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_usage_log_school ON ai_usage_log(school_id, created_at);
