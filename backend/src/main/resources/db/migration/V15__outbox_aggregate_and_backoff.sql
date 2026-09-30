ALTER TABLE outbox_events ADD COLUMN aggregate_id UUID;
ALTER TABLE outbox_events ADD COLUMN next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now();

UPDATE outbox_events
SET aggregate_id = CAST(substring(payload FROM '"bookingId"\s*:\s*"([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})"') AS UUID)
WHERE event_type = 'BOOKING_PAID'
  AND substring(payload FROM '"bookingId"\s*:\s*"([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})"') IS NOT NULL;

-- The old repair job re-enqueued BOOKING_PAID every few minutes while the queue was stuck.
UPDATE outbox_events newer
SET status = 'PROCESSED',
    processed_at = now(),
    last_error = 'Duplicate of an earlier pending event for the same booking.'
WHERE newer.status = 'PENDING'
  AND newer.event_type = 'BOOKING_PAID'
  AND newer.aggregate_id IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM outbox_events older
      WHERE older.status = 'PENDING'
        AND older.event_type = 'BOOKING_PAID'
        AND older.aggregate_id = newer.aggregate_id
        AND (older.created_at, older.id) < (newer.created_at, newer.id)
  );

DROP INDEX IF EXISTS idx_outbox_status_created_at;
CREATE INDEX idx_outbox_events_status_next_attempt ON outbox_events(status, next_attempt_at);
CREATE INDEX idx_outbox_events_aggregate ON outbox_events(aggregate_id, event_type, status);
