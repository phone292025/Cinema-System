UPDATE payments p
SET status = 'REFUNDED'
FROM bookings b
WHERE b.id = p.booking_id
  AND b.status = 'REFUNDED'
  AND p.status = 'SUCCEEDED';

CREATE INDEX IF NOT EXISTS idx_payments_status_paid_at ON payments(status, paid_at);
