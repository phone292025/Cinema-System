package com.cinema.auth;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    long deleteByExpiresAtBeforeOrRevokedAtBefore(Instant expiredBefore, Instant revokedBefore);
}
