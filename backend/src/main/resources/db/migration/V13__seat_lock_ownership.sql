-- Records which booking currently holds a showtime seat.
-- Without this, an expiring booking could release a seat that a newer booking had
-- already taken over, which allowed the same seat to be sold twice.
ALTER TABLE showtime_seats ADD COLUMN locked_by_booking_id UUID;

ALTER TABLE showtime_seats
    ADD CONSTRAINT fk_showtime_seats_locked_by_booking
    FOREIGN KEY (locked_by_booking_id) REFERENCES bookings(id) ON DELETE SET NULL;

CREATE INDEX idx_showtime_seats_locked_by_booking ON showtime_seats(locked_by_booking_id);

-- Backfill ownership for seats that are currently held by a live booking so the
-- new guards do not treat existing rows as unowned.
UPDATE showtime_seats ss
SET locked_by_booking_id = b.id
FROM bookings b
JOIN booking_items bi ON bi.booking_id = b.id
WHERE bi.seat_id = ss.seat_id
  AND b.showtime_id = ss.showtime_id
  AND ss.status IN ('LOCKED', 'BOOKED')
  AND b.status IN ('LOCKED', 'PAYMENT_PENDING', 'PAID', 'TICKET_ISSUED');
