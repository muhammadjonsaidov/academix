-- academix_backend_tdd.md §4.1 table 15 / academix_tz.md §1.16 — real spec table. Not RLS-enabled
-- (user_id is globally unique, no cross-tenant query ever needed here).
CREATE TABLE telegram_connections (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    telegram_chat_id BIGINT NOT NULL,
    telegram_username VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    connected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
