package com.cinema.booking;

public class IllegalBookingStateTransitionException extends RuntimeException {
    public IllegalBookingStateTransitionException(BookingStatus from, BookingEvent event) {
        super("Booking cannot transition from " + from + " with event " + event);
    }
}
