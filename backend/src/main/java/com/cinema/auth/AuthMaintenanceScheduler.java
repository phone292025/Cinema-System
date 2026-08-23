package com.cinema.auth;

import java.time.Instant;

import com.cinema.common.SchedulerGuard;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuthMaintenanceScheduler {
    private final RefreshTokenRepository refreshTokens;
    private final SchedulerGuard schedulerGuard;

    public AuthMaintenanceScheduler(RefreshTokenRepository refreshTokens, SchedulerGuard schedulerGuard) {
        this.refreshTokens = refreshTokens;
        this.schedulerGuard = schedulerGuard;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldRefreshTokensJob() {
        schedulerGuard.runExclusively("refresh-token-cleanup", this::cleanupOldRefreshTokens);
    }

    @Transactional
    public void cleanupOldRefreshTokens() {
        Instant now = Instant.now();
        refreshTokens.deleteByExpiresAtBeforeOrRevokedAtBefore(now, now.minusSeconds(24 * 60 * 60));
    }
}
