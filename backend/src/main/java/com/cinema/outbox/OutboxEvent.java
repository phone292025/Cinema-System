package com.cinema.outbox;

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
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String eventType;
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status = OutboxStatus.PENDING;

    private Integer retryCount = 0;
    private Instant createdAt = Instant.now();
    private Instant processedAt;
    private String lastError;

    public void markProcessed() {
        status = OutboxStatus.PROCESSED;
        processedAt = Instant.now();
        lastError = null;
    }

    public void markFailed(String error) {
        status = OutboxStatus.FAILED;
        lastError = error;
    }

    public void incrementRetry(String error) {
        retryCount = retryCount == null ? 1 : retryCount + 1;
        lastError = error;
    }
}
