package com.cinema.showtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.cinema.common.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SeatEventPublisherTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final UUID showtimeId = UUID.randomUUID();

    @Test
    void fansCommittedEventsOutThroughRedis() throws Exception {
        SeatEventPublisher publisher = publisher(10);
        SeatEvent event = event();

        publisher.afterCommit(event);

        verify(redis).convertAndSend(SeatEventPublisher.CHANNEL, objectMapper.writeValueAsString(event));
    }

    @Test
    void deliversLocallyWhenRedisIsUnavailable() {
        SeatEventPublisher publisher = publisher(10);
        publisher.connect(showtimeId);
        when(redis.convertAndSend(eq(SeatEventPublisher.CHANNEL), anyString())).thenThrow(new RedisConnectionFailureException("down"));

        publisher.afterCommit(event());

        assertThat(publisher.subscriberCount(showtimeId)).isEqualTo(1);
    }

    @Test
    void ignoresMalformedMessagesFromRedis() {
        SeatEventPublisher publisher = publisher(10);
        publisher.connect(showtimeId);

        publisher.onMessage(new DefaultMessage(SeatEventPublisher.CHANNEL.getBytes(StandardCharsets.UTF_8),
                "not json".getBytes(StandardCharsets.UTF_8)), null);

        assertThat(publisher.subscriberCount(showtimeId)).isEqualTo(1);
    }

    @Test
    void refusesSubscribersBeyondThePerShowtimeCap() {
        SeatEventPublisher publisher = publisher(2);
        publisher.connect(showtimeId);
        publisher.connect(showtimeId);

        assertThatThrownBy(() -> publisher.connect(showtimeId)).isInstanceOf(ApiException.class);
        assertThat(publisher.subscriberCount(showtimeId)).isEqualTo(2);
    }

    @Test
    void forgetsAShowtimeOnceItsLastSubscriberLeaves() {
        SeatEventPublisher publisher = publisher(10);
        SseEmitter emitter = publisher.connect(showtimeId);

        emitter.complete();
        publisher.heartbeat();

        assertThat(publisher.trackedShowtimes()).isZero();
    }

    private SeatEventPublisher publisher(int cap) {
        return new SeatEventPublisher(mock(ApplicationEventPublisher.class), redis, objectMapper, cap);
    }

    private SeatEvent event() {
        return new SeatEvent(SeatEventType.SEAT_LOCKED, showtimeId, UUID.randomUUID(), "A1", ShowtimeSeatStatus.LOCKED,
                new BigDecimal("15.00"), null);
    }
}
