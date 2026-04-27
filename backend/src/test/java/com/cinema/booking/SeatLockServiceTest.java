package com.cinema.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class SeatLockServiceTest {
    @Test
    void shouldReleaseAcquiredLocksWhenOneSeatCannotBeLocked() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);

        UUID showtimeId = UUID.randomUUID();
        UUID seatA = UUID.randomUUID();
        UUID seatB = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        String keyA = "lock:showtime:%s:seat:%s".formatted(showtimeId, seatA);
        when(values.setIfAbsent(eq(keyA), eq(bookingId.toString()), any(Duration.class))).thenReturn(true);
        when(values.setIfAbsent(eq("lock:showtime:%s:seat:%s".formatted(showtimeId, seatB)), eq(bookingId.toString()), any(Duration.class)))
                .thenReturn(false);

        SeatLockService service = new SeatLockService(redis);
        List<String> acquired = service.lock(showtimeId, List.of(seatA, seatB), bookingId, Duration.ofMinutes(5));

        assertThat(acquired).isEmpty();
        verify(redis).delete(List.of(keyA));
    }
}
