package com.cinema.ticket;

import java.util.List;
import java.util.Map;

import com.cinema.booking.BookingRepository;
import com.cinema.booking.BookingStatus;
import com.cinema.common.SchedulerGuard;
import com.cinema.outbox.OutboxService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TicketRepairScheduler {
    private final BookingRepository bookings;
    private final TicketRepository tickets;
    private final OutboxService outbox;
    private final SchedulerGuard schedulerGuard;

    public TicketRepairScheduler(BookingRepository bookings, TicketRepository tickets, OutboxService outbox, SchedulerGuard schedulerGuard) {
        this.bookings = bookings;
        this.tickets = tickets;
        this.outbox = outbox;
        this.schedulerGuard = schedulerGuard;
    }

    @Scheduled(fixedDelay = 5 * 60_000)
    public void repairMissingTicketsJob() {
        schedulerGuard.runExclusively("ticket-repair", this::repairMissingTickets);
    }

    @Transactional
    public void repairMissingTickets() {
        bookings.findByStatusIn(List.of(BookingStatus.PAID, BookingStatus.TICKET_ISSUED)).stream()
                .filter(booking -> tickets.findByBookingId(booking.getId()).isEmpty())
                .forEach(booking -> outbox.enqueue("BOOKING_PAID", Map.of("bookingId", booking.getId().toString())));
    }
}
