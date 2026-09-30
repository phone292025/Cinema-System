package com.cinema.notification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findTop100ByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndReadAtIsNull(UUID userId);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("update Notification n set n.read = true, n.readAt = :readAt where n.user.id = :userId and n.readAt is null")
    int markAllRead(@Param("userId") UUID userId, @Param("readAt") Instant readAt);
}
