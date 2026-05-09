UPDATE bookings
SET status = 'PAID'
WHERE status = 'TICKET_ISSUED'
AND NOT EXISTS (
    SELECT 1
    FROM tickets
    WHERE tickets.booking_id = bookings.id
);
