package com.cinema.ticket;

import java.util.List;
import java.util.Map;

import com.cinema.booking.BookingStatus;
import com.cinema.common.SchedulerGuard;
import com.cinema.outbox.OutboxEvent;
import com.cinema.outbox.OutboxService;
import com.cinema.outbox.OutboxStatus;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TicketRepairScheduler {
    private final TicketRepository tickets;
    private final OutboxService outbox;
    private final SchedulerGuard schedulerGuard;

    public TicketRepairScheduler(TicketRepository tickets, OutboxService outbox, SchedulerGuard schedulerGuard) {
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
        tickets.findBookingsMissingTicket(List.of(BookingStatus.PAID, BookingStatus.TICKET_ISSUED), OutboxEvent.BOOKING_PAID,
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED))
                .forEach(bookingId -> outbox.enqueue(OutboxEvent.BOOKING_PAID, bookingId, Map.of("bookingId", bookingId.toString())));
    }
}
