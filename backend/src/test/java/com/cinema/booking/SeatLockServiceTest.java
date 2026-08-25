package com.cinema.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

class SeatLockServiceTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final SeatLockService service = new SeatLockService(redis);

    private final UUID showtimeId = UUID.randomUUID();
    private final UUID seatA = UUID.randomUUID();
    private final UUID seatB = UUID.randomUUID();
    private final UUID bookingId = UUID.randomUUID();

    @Test
    void shouldReleaseAcquiredLocksWhenOneSeatCannotBeLocked() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(eq(service.key(showtimeId, seatA)), eq(bookingId.toString()), any(Duration.class))).thenReturn(true);
        when(values.setIfAbsent(eq(service.key(showtimeId, seatB)), eq(bookingId.toString()), any(Duration.class))).thenReturn(false);

        List<String> acquired = service.lock(showtimeId, List.of(seatA, seatB), bookingId, Duration.ofMinutes(5));

        assertThat(acquired).isEmpty();
        verify(redis).execute(any(RedisScript.class), eq(List.of(service.key(showtimeId, seatA))), eq(bookingId.toString()));
        verify(redis, never()).delete(anyList());
    }

    @Test
    void shouldReleaseOnlyLocksOwnedByTheBooking() {
        when(redis.execute(any(RedisScript.class), anyList(), eq(bookingId.toString()))).thenReturn(1L);

        long released = service.release(showtimeId, List.of(seatA, seatB), bookingId);

        assertThat(released).isEqualTo(1L);
        verify(redis).execute(any(RedisScript.class),
                eq(List.of(service.key(showtimeId, seatA), service.key(showtimeId, seatB))), eq(bookingId.toString()));
    }

    @Test
    void shouldNotReportSeatLockedByOtherWhenTheBookingItselfHoldsIt() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(service.key(showtimeId, seatA))).thenReturn(bookingId.toString());

        assertThat(service.isLockedByOther(showtimeId, seatA, bookingId)).isFalse();
    }

    @Test
    void shouldReportSeatLockedByOtherWhenAnotherBookingHoldsIt() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(service.key(showtimeId, seatA))).thenReturn(UUID.randomUUID().toString());

        assertThat(service.isLockedByOther(showtimeId, seatA, bookingId)).isTrue();
    }
}
