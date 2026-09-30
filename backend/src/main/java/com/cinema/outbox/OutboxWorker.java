package com.cinema.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cinema.common.SchedulerGuard;
import com.cinema.ticket.TicketService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OutboxWorker {
    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);
    private static final Limit BATCH_SIZE = Limit.of(50);
    private static final int MAX_ERROR_LENGTH = 2000;

    private final OutboxEventRepository events;
    private final TicketService tickets;
    private final SchedulerGuard schedulerGuard;
    private final TransactionTemplate ownTransaction;
    private final int maxAttempts;
    private final Duration retryBackoff;

    public OutboxWorker(OutboxEventRepository events, TicketService tickets, SchedulerGuard schedulerGuard,
            PlatformTransactionManager transactionManager,
            @Value("${app.outbox.max-attempts:5}") int maxAttempts,
            @Value("${app.outbox.retry-backoff-seconds:30}") long retryBackoffSeconds) {
        this.events = events;
        this.tickets = tickets;
        this.schedulerGuard = schedulerGuard;
        this.ownTransaction = new TransactionTemplate(transactionManager);
        this.ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.maxAttempts = maxAttempts;
        this.retryBackoff = Duration.ofSeconds(retryBackoffSeconds);
    }

    @Scheduled(fixedDelay = 5_000)
    public void processOutboxEventsJob() {
        schedulerGuard.runExclusively("outbox-dispatch", this::processOutboxEvents);
    }

    public void processOutboxEvents() {
        List<UUID> dueIds = ownTransaction.execute(status -> events.findDueIds(OutboxStatus.PENDING, Instant.now(), BATCH_SIZE));
        for (UUID eventId : dueIds) {
            process(eventId);
        }
    }

    private void process(UUID eventId) {
        try {
            ownTransaction.executeWithoutResult(status -> events.lockById(eventId)
                    .filter(event -> event.isDue(Instant.now()))
                    .ifPresent(event -> {
                        dispatch(event);
                        event.markProcessed();
                    }));
        } catch (RuntimeException ex) {
            recordFailure(eventId, ex);
        }
    }

    private void dispatch(OutboxEvent event) {
        if (OutboxEvent.BOOKING_PAID.equals(event.getEventType())) {
            if (event.getAggregateId() == null) {
                throw new IllegalStateException("BOOKING_PAID event has no booking id.");
            }
            tickets.issue(event.getAggregateId());
            return;
        }
        throw new IllegalArgumentException("Unknown outbox event type: " + event.getEventType());
    }

    private void recordFailure(UUID eventId, RuntimeException failure) {
        String error = describe(failure);
        ownTransaction.executeWithoutResult(status -> events.findById(eventId).ifPresent(event -> {
            event.recordFailure(error, maxAttempts, retryBackoff, Instant.now());
            if (event.getStatus() == OutboxStatus.FAILED) {
                log.error("Outbox event {} ({}) failed permanently after {} attempts.", eventId, event.getEventType(),
                        event.getRetryCount(), failure);
            } else {
                log.warn("Outbox event {} ({}) failed on attempt {}; next attempt at {}.", eventId, event.getEventType(),
                        event.getRetryCount(), event.getNextAttemptAt(), failure);
            }
        }));
    }

    private String describe(RuntimeException failure) {
        String error = failure.getClass().getSimpleName() + ": " + failure.getMessage();
        return error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error;
    }
}
