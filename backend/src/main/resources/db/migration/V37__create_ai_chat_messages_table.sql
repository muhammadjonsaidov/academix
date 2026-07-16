-- academix_tz.md §3.4 / §8's exact DDL. One of the 9 RLS-enabled tables (backend_tdd.md §4.1.1 /
-- CLAUDE.md architecture section) — never built until now (AI Tutor chat itself was 100% unbuilt).
CREATE TABLE ai_chat_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE, -- RLS uchun (bo'lim 5.4)
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject VARCHAR(30) NOT NULL,
    message TEXT NOT NULL,
    response TEXT NOT NULL,
    is_blocked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE ai_chat_messages ENABLE ROW LEVEL SECURITY;
CREATE POLICY school_isolation ON ai_chat_messages
  USING (school_id = current_setting('app.current_school_id')::uuid);

CREATE INDEX idx_chat_student_subject ON ai_chat_messages(student_id, subject, created_at);
