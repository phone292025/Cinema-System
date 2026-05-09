ALTER TABLE showtime_seats
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

UPDATE bookings
SET status = 'PAYMENT_PENDING'
WHERE status = 'PENDING';

UPDATE bookings
SET status = 'TICKET_ISSUED'
WHERE status = 'PAID'
AND EXISTS (
    SELECT 1
    FROM payments
    WHERE payments.booking_id = bookings.id
    AND payments.status = 'SUCCEEDED'
);

UPDATE payments
SET status = 'PENDING'
WHERE status = 'PROCESSING';
