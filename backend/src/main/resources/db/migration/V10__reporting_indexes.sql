CREATE INDEX IF NOT EXISTS idx_bookings_status_created_at ON bookings(status, created_at);
CREATE INDEX IF NOT EXISTS idx_payments_status_created_at ON payments(status, created_at);
CREATE INDEX IF NOT EXISTS idx_showtimes_start_time ON showtimes(start_time);
CREATE INDEX IF NOT EXISTS idx_booking_items_seat ON booking_items(seat_id);
