package com.cinema.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {
    public static final String BOOKING_PAID = "BOOKING_PAID";

    private static final int MAX_BACKOFF_DOUBLINGS = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String eventType;
    private UUID aggregateId;
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status = OutboxStatus.PENDING;

    private Integer retryCount = 0;
    private Instant createdAt = Instant.now();
    private Instant nextAttemptAt = createdAt;
    private Instant processedAt;
    private String lastError;

    public boolean isDue(Instant now) {
        return status == OutboxStatus.PENDING && !nextAttemptAt.isAfter(now);
    }

    public void markProcessed() {
        status = OutboxStatus.PROCESSED;
        processedAt = Instant.now();
        lastError = null;
    }

    public void recordFailure(String error, int maxAttempts, Duration baseBackoff, Instant now) {
        retryCount = retryCount == null ? 1 : retryCount + 1;
        lastError = error;
        if (retryCount >= maxAttempts) {
            status = OutboxStatus.FAILED;
            return;
        }
        long multiplier = 1L << Math.min(retryCount - 1, MAX_BACKOFF_DOUBLINGS);
        nextAttemptAt = now.plus(baseBackoff.multipliedBy(multiplier));
    }
}
