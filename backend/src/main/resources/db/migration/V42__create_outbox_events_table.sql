-- Transactional outbox: an application write and its integration event commit atomically.
-- The dispatcher is a trusted backend job that must read every tenant's pending events; school_id
-- remains in each record for tenant-aware consumers.
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    destination VARCHAR(180) NOT NULL,
    payload_type VARCHAR(80) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    available_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    attempts INT NOT NULL DEFAULT 0,
    last_error TEXT
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events (available_at, occurred_at)
    WHERE published_at IS NULL;
