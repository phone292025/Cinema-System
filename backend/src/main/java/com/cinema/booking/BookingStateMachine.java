package com.cinema.booking;

import java.util.EnumMap;
import java.util.Map;

public class BookingStateMachine {
    private final Map<BookingStatus, Map<BookingEvent, BookingStatus>> transitions = new EnumMap<>(BookingStatus.class);

    public BookingStateMachine() {
        allow(BookingStatus.LOCKED, BookingEvent.PAYMENT_STARTED, BookingStatus.PAYMENT_PENDING);
        allow(BookingStatus.LOCKED, BookingEvent.EXPIRED, BookingStatus.EXPIRED);
        allow(BookingStatus.LOCKED, BookingEvent.CANCELLED, BookingStatus.CANCELLED);

        allow(BookingStatus.PAYMENT_PENDING, BookingEvent.PAYMENT_SUCCEEDED, BookingStatus.PAID);
        allow(BookingStatus.PAYMENT_PENDING, BookingEvent.PAYMENT_FAILED, BookingStatus.CANCELLED);
        allow(BookingStatus.PAYMENT_PENDING, BookingEvent.EXPIRED, BookingStatus.EXPIRED);
        allow(BookingStatus.PAYMENT_PENDING, BookingEvent.CANCELLED, BookingStatus.CANCELLED);

        allow(BookingStatus.PAID, BookingEvent.TICKET_ISSUED, BookingStatus.TICKET_ISSUED);
        allow(BookingStatus.PAID, BookingEvent.CANCELLED, BookingStatus.CANCELLED);
        allow(BookingStatus.PAID, BookingEvent.REFUND_REQUESTED, BookingStatus.REFUND_PENDING);

        allow(BookingStatus.TICKET_ISSUED, BookingEvent.CANCELLED, BookingStatus.CANCELLED);
        allow(BookingStatus.TICKET_ISSUED, BookingEvent.REFUND_REQUESTED, BookingStatus.REFUND_PENDING);

        allow(BookingStatus.REFUND_PENDING, BookingEvent.REFUNDED, BookingStatus.REFUNDED);
    }

    public BookingStatus transition(BookingStatus from, BookingEvent event) {
        BookingStatus to = transitions.getOrDefault(from, Map.of()).get(event);
        if (to == null) {
            throw new IllegalBookingStateTransitionException(from, event);
        }
        return to;
    }

    public boolean canTransition(BookingStatus from, BookingEvent event) {
        return transitions.getOrDefault(from, Map.of()).containsKey(event);
    }

    private void allow(BookingStatus from, BookingEvent event, BookingStatus to) {
        transitions.computeIfAbsent(from, ignored -> new EnumMap<>(BookingEvent.class)).put(event, to);
    }
}
