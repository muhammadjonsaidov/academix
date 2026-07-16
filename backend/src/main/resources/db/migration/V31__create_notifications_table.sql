-- academix_tz.md §1.15 — entity fields are fully spec'd, but no CREATE TABLE DDL exists anywhere
-- in academix_backend_tdd.md's §4.1 schema listing (only the Telegram webhook security note
-- mentions this table in passing) — deviation: DDL derived directly from the given Java fields.
-- Not RLS-enabled — scoped by user_id directly, no cross-tenant query ever needed here.
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL CHECK (type IN (
        'HOMEWORK_ASSIGNED', 'DEADLINE_REMINDER', 'HOMEWORK_GRADED', 'STREAK_BROKEN',
        'STREAK_MILESTONE', 'BADGE_EARNED', 'PSYCHOLOGICAL_ALERT', 'LATE_SUBMISSION',
        'CLASS_PROGRESS_REPORT', 'HANDWRITING_PROFILE_RESET', 'AI_BUDGET_LOW')),
    title VARCHAR(255) NOT NULL,
    body TEXT,
    data JSONB,
    is_read BOOLEAN DEFAULT FALSE,
    sent_to_telegram BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);
