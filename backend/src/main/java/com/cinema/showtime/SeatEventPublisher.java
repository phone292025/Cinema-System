package com.cinema.showtime;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.cinema.common.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SeatEventPublisher implements MessageListener {
    public static final String CHANNEL = "cinema:seat-events";
    private static final Logger log = LoggerFactory.getLogger(SeatEventPublisher.class);
    private static final long SSE_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final ApplicationEventPublisher events;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final int maxSubscribersPerShowtime;
    private final Map<UUID, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SeatEventPublisher(ApplicationEventPublisher events, StringRedisTemplate redis, ObjectMapper objectMapper,
            @Value("${app.seat-events.max-subscribers-per-showtime:500}") int maxSubscribersPerShowtime) {
        this.events = events;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.maxSubscribersPerShowtime = maxSubscribersPerShowtime;
    }

    public SseEmitter connect(UUID showtimeId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitters.compute(showtimeId, (id, subscribers) -> {
            Set<SseEmitter> current = subscribers == null ? ConcurrentHashMap.newKeySet() : subscribers;
            if (current.size() >= maxSubscribersPerShowtime) {
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Too many people are watching this showtime live. Refresh to see the latest seats.");
            }
            current.add(emitter);
            return current;
        });
        emitter.onCompletion(() -> remove(showtimeId, emitter));
        emitter.onTimeout(() -> remove(showtimeId, emitter));
        emitter.onError(ignored -> remove(showtimeId, emitter));
        send(showtimeId, emitter, SseEmitter.event().name("CONNECTED").data("connected"));
        return emitter;
    }

    public void publish(SeatEvent event) {
        events.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(SeatEvent event) {
        try {
            redis.convertAndSend(CHANNEL, objectMapper.writeValueAsString(event));
        } catch (DataAccessException | JsonProcessingException ex) {
            log.debug("Could not fan seat event out through Redis, delivering locally: {}", ex.getMessage());
            broadcast(event);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            broadcast(objectMapper.readValue(new String(message.getBody(), StandardCharsets.UTF_8), SeatEvent.class));
        } catch (IOException ex) {
            log.warn("Ignoring malformed seat event from Redis: {}", ex.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${app.seat-events.heartbeat-millis:20000}")
    public void heartbeat() {
        emitters.forEach((showtimeId, subscribers) ->
                subscribers.forEach(emitter -> send(showtimeId, emitter, SseEmitter.event().comment("heartbeat"))));
    }

    int subscriberCount(UUID showtimeId) {
        Set<SseEmitter> subscribers = emitters.get(showtimeId);
        return subscribers == null ? 0 : subscribers.size();
    }

    int trackedShowtimes() {
        return emitters.size();
    }

    void broadcast(SeatEvent event) {
        Set<SseEmitter> subscribers = emitters.get(event.showtimeId());
        if (subscribers == null) {
            return;
        }
        subscribers.forEach(emitter -> send(event.showtimeId(), emitter, SseEmitter.event().name(event.type().name()).data(event)));
    }

    private void send(UUID showtimeId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException ex) {
            remove(showtimeId, emitter);
        }
    }

    private void remove(UUID showtimeId, SseEmitter emitter) {
        emitters.computeIfPresent(showtimeId, (id, subscribers) -> {
            subscribers.remove(emitter);
            return subscribers.isEmpty() ? null : subscribers;
        });
    }
}
