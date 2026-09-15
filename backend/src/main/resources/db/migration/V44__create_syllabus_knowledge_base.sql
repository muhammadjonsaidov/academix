-- Each source paragraph/chunk is stored separately so semantic retrieval can ground downstream
-- lesson plans and unique homework in the exact uploaded material instead of a whole PDF prompt.
ALTER TABLE teacher_syllabuses
    ADD COLUMN processing_status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CHECK (processing_status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED')),
    ADD COLUMN processing_error TEXT,
    ADD COLUMN processed_at TIMESTAMP;

CREATE TABLE syllabus_chunks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    syllabus_id UUID NOT NULL REFERENCES teacher_syllabuses(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES school_classes(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER NOT NULL,
    embedding vector(1024) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (syllabus_id, chunk_index)
);

CREATE INDEX idx_syllabus_chunks_scope
    ON syllabus_chunks (teacher_id, subject_id, class_id, syllabus_id);
CREATE INDEX idx_syllabus_chunks_embedding_cosine
    ON syllabus_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 50);
