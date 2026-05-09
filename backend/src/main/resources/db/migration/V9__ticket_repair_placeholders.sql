INSERT INTO outbox_events (event_type, payload, status)
SELECT 'BOOKING_PAID', '{"bookingId":"' || b.id || '"}', 'PENDING'
FROM bookings b
WHERE b.status IN ('PAID', 'TICKET_ISSUED')
AND NOT EXISTS (
    SELECT 1
    FROM tickets t
    WHERE t.booking_id = b.id
)
AND NOT EXISTS (
    SELECT 1
    FROM outbox_events oe
    WHERE oe.event_type = 'BOOKING_PAID'
    AND oe.payload = '{"bookingId":"' || b.id || '"}'
);
