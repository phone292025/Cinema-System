ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ;

UPDATE notifications
SET read_at = created_at
WHERE is_read = TRUE
AND read_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_user_read_at ON notifications(user_id, read_at);
