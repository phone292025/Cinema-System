package com.cinema.booking;

public enum BookingEvent {
    LOCK_CREATED,
    PAYMENT_STARTED,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    TICKET_ISSUED,
    EXPIRED,
    CANCELLED,
    REFUND_REQUESTED,
    REFUNDED
}
