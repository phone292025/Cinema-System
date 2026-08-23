package com.cinema.booking;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

/**
 * Fast cross-instance hint about which seats are being held right now.
 * <p>
 * Redis is deliberately <strong>not</strong> the source of truth: the authoritative
 * lock is the {@code SELECT ... FOR UPDATE} row lock plus the status of the
 * {@code showtime_seats} row. Redis only shortens the window in which two users
 * see the same seat as free, so a Redis outage degrades responsiveness rather
 * than correctness.
 * <p>
 * Every delete is a compare-and-delete against the owning booking id, so a booking
 * can only ever release a lock it still holds.
 */
@Service
public class SeatLockService {
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

    public SeatLockService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String key(UUID showtimeId, UUID seatId) {
        return "lock:showtime:%s:seat:%s".formatted(showtimeId, seatId);
    }

    public boolean isLocked(UUID showtimeId, UUID seatId) {
        return Boolean.TRUE.equals(redis.hasKey(key(showtimeId, seatId)));
    }

    /** True only when some <em>other</em> booking holds the seat. */
    public boolean isLockedByOther(UUID showtimeId, UUID seatId, UUID bookingId) {
        String holder = redis.opsForValue().get(key(showtimeId, seatId));
        return holder != null && !holder.equals(bookingId.toString());
    }

    public List<String> lock(UUID showtimeId, List<UUID> seatIds, UUID bookingId, Duration ttl) {
        List<String> acquired = new ArrayList<>();
        for (UUID seatId : seatIds) {
            String key = key(showtimeId, seatId);
            Boolean ok = redis.opsForValue().setIfAbsent(key, bookingId.toString(), ttl);
            if (!Boolean.TRUE.equals(ok)) {
                releaseKeys(acquired, bookingId);
                return List.of();
            }
            acquired.add(key);
        }
        return acquired;
    }

    /**
     * Releases the seats only if {@code bookingId} still owns them.
     *
     * @return how many locks were actually released
     */
    public long release(UUID showtimeId, List<UUID> seatIds, UUID bookingId) {
        return releaseKeys(seatIds.stream().map(seatId -> key(showtimeId, seatId)).toList(), bookingId);
    }

    private long releaseKeys(List<String> keys, UUID bookingId) {
        if (keys.isEmpty()) {
            return 0;
        }
        Long released = redis.execute(RELEASE_IF_OWNED, keys, bookingId.toString());
        return released == null ? 0 : released;
    }
}
