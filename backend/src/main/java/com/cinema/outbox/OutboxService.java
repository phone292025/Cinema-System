package com.cinema.outbox;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {
    private final OutboxEventRepository events;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository events, ObjectMapper objectMapper) {
        this.events = events;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueue(String eventType, UUID aggregateId, Object payload) {
        try {
            OutboxEvent event = new OutboxEvent();
            event.setEventType(eventType);
            event.setAggregateId(aggregateId);
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setStatus(OutboxStatus.PENDING);
            events.save(event);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to enqueue outbox event", ex);
        }
    }
}
