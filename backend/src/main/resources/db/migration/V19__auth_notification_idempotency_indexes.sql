CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_created_at ON notifications(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_idempotency_completed_at ON idempotency_keys(completed_at);
