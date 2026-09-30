package com.cinema.idempotency;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {
    Optional<IdempotencyKey> findByActorKeyAndKeyValue(String actorKey, String keyValue);

    @Modifying
    @Query("delete from IdempotencyKey k where k.completedAt is null and k.createdAt < :cutoff")
    int deleteAbandonedBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("delete from IdempotencyKey k where k.completedAt < :cutoff")
    int deleteCompletedBefore(@Param("cutoff") Instant cutoff);
}
