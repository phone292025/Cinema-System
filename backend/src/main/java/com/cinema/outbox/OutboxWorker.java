package com.cinema.outbox;

import java.util.UUID;

import com.cinema.ticket.TicketService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxWorker {
    private final OutboxEventRepository events;
    private final ObjectMapper objectMapper;
    private final TicketService tickets;

    public OutboxWorker(OutboxEventRepository events, ObjectMapper objectMapper, TicketService tickets) {
        this.events = events;
        this.objectMapper = objectMapper;
        this.tickets = tickets;
    }

    @Scheduled(fixedDelay = 5_000)
    @Transactional
    public void processOutboxEvents() {
        for (OutboxEvent event : events.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)) {
            try {
                dispatch(event);
                event.markProcessed();
            } catch (Exception ex) {
                event.incrementRetry(ex.getMessage());
                if (event.getRetryCount() >= 3) {
                    event.markFailed(ex.getMessage());
                }
            }
        }
    }

    private void dispatch(OutboxEvent event) throws Exception {
        JsonNode payload = objectMapper.readTree(event.getPayload());
        if ("BOOKING_PAID".equals(event.getEventType())) {
            tickets.issue(UUID.fromString(payload.get("bookingId").asText()));
            return;
        }
        throw new IllegalArgumentException("Unknown outbox event type: " + event.getEventType());
    }
}
