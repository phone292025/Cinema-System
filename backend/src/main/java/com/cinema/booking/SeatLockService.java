package com.cinema.booking;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
public class SeatLockService {
    private static final Logger log = LoggerFactory.getLogger(SeatLockService.class);
    private static final long WARNING_INTERVAL_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final RedisScript<Long> RELEASE_IF_OWNED = new DefaultRedisScript<>(
            """
            local released = 0
            for i, key in ipairs(KEYS) do
              if redis.call('get', key) == ARGV[1] then
                released = released + redis.call('del', key)
              end
            end
            return released
            """, Long.class);

    private final StringRedisTemplate redis;
    private final AtomicLong lastWarningAt = new AtomicLong();

    public SeatLockService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String key(UUID showtimeId, UUID seatId) {
        return "lock:showtime:%s:seat:%s".formatted(showtimeId, seatId);
    }

    public boolean isLocked(UUID showtimeId, UUID seatId) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(key(showtimeId, seatId)));
        } catch (DataAccessException ex) {
            redisUnavailable("check", ex);
            return false;
        }
    }

    public List<String> lock(UUID showtimeId, List<UUID> seatIds, UUID bookingId, Duration ttl) {
        List<String> keys = seatIds.stream().map(seatId -> key(showtimeId, seatId)).toList();
        List<String> acquired = new ArrayList<>();
        try {
            for (String key : keys) {
                Boolean ok = redis.opsForValue().setIfAbsent(key, bookingId.toString(), ttl);
                if (!Boolean.TRUE.equals(ok)) {
                    releaseKeys(acquired, bookingId);
                    return List.of();
                }
                acquired.add(key);
            }
        } catch (DataAccessException ex) {
            redisUnavailable("lock", ex);
            return keys;
        }
        return acquired;
    }

    public long release(UUID showtimeId, List<UUID> seatIds, UUID bookingId) {
        return releaseKeys(seatIds.stream().map(seatId -> key(showtimeId, seatId)).toList(), bookingId);
    }

    private long releaseKeys(List<String> keys, UUID bookingId) {
        if (keys.isEmpty()) {
            return 0;
        }
        try {
            Long released = redis.execute(RELEASE_IF_OWNED, keys, bookingId.toString());
            return released == null ? 0 : released;
        } catch (DataAccessException ex) {
            redisUnavailable("release", ex);
            return 0;
        }
    }

    private void redisUnavailable(String operation, DataAccessException ex) {
        long now = System.currentTimeMillis();
        long last = lastWarningAt.get();
        if (now - last >= WARNING_INTERVAL_MILLIS && lastWarningAt.compareAndSet(last, now)) {
            log.warn("Redis seat lock {} failed, relying on database row locks until Redis recovers: {}", operation, ex.getMessage());
        } else {
            log.debug("Redis seat lock {} failed: {}", operation, ex.getMessage());
        }
    }
}
