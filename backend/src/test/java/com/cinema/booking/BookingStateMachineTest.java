package com.cinema.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BookingStateMachineTest {
    private final BookingStateMachine stateMachine = new BookingStateMachine();

    @Test
    void movesPaidBookingIntoTicketIssuedState() {
        BookingStatus paymentPending = stateMachine.transition(BookingStatus.LOCKED, BookingEvent.PAYMENT_STARTED);
        BookingStatus paid = stateMachine.transition(paymentPending, BookingEvent.PAYMENT_SUCCEEDED);

        assertThat(stateMachine.transition(paid, BookingEvent.TICKET_ISSUED)).isEqualTo(BookingStatus.TICKET_ISSUED);
    }

    @Test
    void rejectsIllegalBackwardTransition() {
        assertThatThrownBy(() -> stateMachine.transition(BookingStatus.PAID, BookingEvent.PAYMENT_STARTED))
                .isInstanceOf(IllegalBookingStateTransitionException.class);
    }
}
