-- Per-user, per-type notification channel toggles (deviation — no preferences concept exists in
-- academix_tz.md; the notifications inbox itself is already a flagged deviation). A missing row
-- means "both channels enabled" (default-on). Not RLS-enabled — user-scoped like notifications,
-- no school_id column at all.
CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    telegram_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (user_id, type)
);

CREATE INDEX idx_notification_prefs_user ON notification_preferences(user_id);
