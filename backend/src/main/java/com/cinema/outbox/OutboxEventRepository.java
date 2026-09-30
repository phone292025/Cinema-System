package com.cinema.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query("""
            select e.id from OutboxEvent e
            where e.status = :status and e.nextAttemptAt <= :now
            order by e.nextAttemptAt, e.createdAt
            """)
    List<UUID> findDueIds(@Param("status") OutboxStatus status, @Param("now") Instant now, Limit limit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OutboxEvent e where e.id = :id")
    Optional<OutboxEvent> lockById(@Param("id") UUID id);
}
