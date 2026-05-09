package com.cinema.showtime;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SeatEventPublisher {
    private static final long SSE_TIMEOUT = 30L * 60L * 1000L;

    private final ApplicationEventPublisher events;
    private final ConcurrentHashMap<UUID, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SeatEventPublisher(ApplicationEventPublisher events) {
        this.events = events;
    }

    public SseEmitter connect(UUID showtimeId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.computeIfAbsent(showtimeId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitter.onCompletion(() -> remove(showtimeId, emitter));
        emitter.onTimeout(() -> remove(showtimeId, emitter));
        emitter.onError(ignored -> remove(showtimeId, emitter));
        try {
            emitter.send(SseEmitter.event().name("CONNECTED").data("connected"));
        } catch (IOException ex) {
            remove(showtimeId, emitter);
        }
        return emitter;
    }

    public void publish(SeatEvent event) {
        events.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(SeatEvent event) {
        broadcast(event);
    }

    private void broadcast(SeatEvent event) {
        Set<SseEmitter> subscribers = emitters.get(event.showtimeId());
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }
        subscribers.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(event.type().name()).data(event));
            } catch (IOException ex) {
                remove(event.showtimeId(), emitter);
            }
        });
    }

    private void remove(UUID showtimeId, SseEmitter emitter) {
        Set<SseEmitter> subscribers = emitters.get(showtimeId);
        if (subscribers != null) {
            subscribers.remove(emitter);
        }
    }
}
