package com.cinema.idempotency;

import java.time.Duration;
import java.time.Instant;

import com.cinema.common.ApiException;
import com.cinema.user.User;
import com.cinema.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {
    private final IdempotencyKeyRepository keys;
    private final UserRepository users;

    public IdempotencyService(IdempotencyKeyRepository keys, UserRepository users) {
        this.keys = keys;
        this.users = users;
    }

    @Transactional
    public CachedResponse checkOrCreate(String key, String actorKey, String userId, String requestHash) {
        return keys.findByActorKeyAndKeyValue(actorKey, key)
                .map(existing -> cached(existing, requestHash))
                .orElseGet(() -> create(key, actorKey, userId, requestHash));
    }

    @Transactional
    public void storeResponse(String key, String actorKey, int statusCode, String responseBody) {
        IdempotencyKey record = keys.findByActorKeyAndKeyValue(actorKey, key)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Idempotency record missing."));
        record.setStatusCode(statusCode);
        record.setResponseBody(responseBody);
        record.setCompletedAt(Instant.now());
    }

    @Transactional
    public void deletePending(String key, String actorKey) {
        keys.findByActorKeyAndKeyValue(actorKey, key)
                .filter(record -> record.getCompletedAt() == null)
                .ifPresent(keys::delete);
    }

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void cleanupAbandonedKeys() {
        keys.deleteByCompletedAtIsNullAndCreatedAtBefore(Instant.now().minus(Duration.ofHours(1)));
    }

    private CachedResponse create(String key, String actorKey, String userId, String requestHash) {
        IdempotencyKey record = new IdempotencyKey();
        record.setKeyValue(key);
        record.setActorKey(actorKey);
        record.setRequestHash(requestHash);
        if (userId != null) {
            users.findById(java.util.UUID.fromString(userId)).ifPresent(record::setUser);
        }
        keys.save(record);
        return CachedResponse.miss();
    }

    private CachedResponse cached(IdempotencyKey existing, String requestHash) {
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new ApiException(HttpStatus.CONFLICT, "Idempotency key was reused with a different request.");
        }
        if (existing.getCompletedAt() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "Request with this idempotency key is still processing.");
        }
        return new CachedResponse(true, existing.getStatusCode(), existing.getResponseBody());
    }

    public record CachedResponse(boolean hit, Integer statusCode, String responseBody) {
        static CachedResponse miss() {
            return new CachedResponse(false, null, null);
        }
    }
}
