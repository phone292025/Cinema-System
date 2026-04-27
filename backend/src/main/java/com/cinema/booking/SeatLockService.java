package com.cinema.booking;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SeatLockService {
    private final StringRedisTemplate redis;

    public SeatLockService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String key(UUID showtimeId, UUID seatId) {
        return "lock:showtime:%s:seat:%s".formatted(showtimeId, seatId);
    }

    public boolean isLocked(UUID showtimeId, UUID seatId) {
        return Boolean.TRUE.equals(redis.hasKey(key(showtimeId, seatId)));
    }

    public List<String> lock(UUID showtimeId, List<UUID> seatIds, UUID bookingId, Duration ttl) {
        List<String> acquired = new ArrayList<>();
        for (UUID seatId : seatIds) {
            String key = key(showtimeId, seatId);
            Boolean ok = redis.opsForValue().setIfAbsent(key, bookingId.toString(), ttl);
            if (!Boolean.TRUE.equals(ok)) {
                release(acquired);
                return List.of();
            }
            acquired.add(key);
        }
        return acquired;
    }

    public void release(UUID showtimeId, List<UUID> seatIds) {
        release(seatIds.stream().map(seatId -> key(showtimeId, seatId)).toList());
    }

    public void release(List<String> keys) {
        if (!keys.isEmpty()) {
            redis.delete(keys);
        }
    }
}
