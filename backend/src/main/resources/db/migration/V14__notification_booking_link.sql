-- Notifications are about a specific booking, so record it and let the UI link
-- straight to the ticket instead of leaving people to find it themselves.
ALTER TABLE notifications ADD COLUMN booking_id UUID;

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_booking
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL;

CREATE INDEX idx_notifications_booking ON notifications(booking_id);
