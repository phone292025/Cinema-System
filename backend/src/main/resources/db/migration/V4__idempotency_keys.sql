CREATE TABLE IF NOT EXISTS idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_value VARCHAR(160) NOT NULL,
    actor_key VARCHAR(220) NOT NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    request_hash VARCHAR(128) NOT NULL,
    response_body TEXT,
    status_code INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT uq_idempotency_actor_key UNIQUE (actor_key, key_value)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_created_at ON idempotency_keys(created_at);
