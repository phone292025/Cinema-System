package com.cinema.idempotency;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {
    Optional<IdempotencyKey> findByActorKeyAndKeyValue(String actorKey, String keyValue);

    void deleteByCompletedAtIsNullAndCreatedAtBefore(Instant cutoff);
}
