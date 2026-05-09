package com.cinema.auth;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuthMaintenanceScheduler {
    private final RefreshTokenRepository refreshTokens;

    public AuthMaintenanceScheduler(RefreshTokenRepository refreshTokens) {
        this.refreshTokens = refreshTokens;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldRefreshTokens() {
        Instant now = Instant.now();
        refreshTokens.deleteByExpiresAtBeforeOrRevokedAtBefore(now, now.minusSeconds(24 * 60 * 60));
    }
}
