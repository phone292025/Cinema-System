package com.cinema.common;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs a scheduled job on at most one instance at a time.
 * <p>
 * Every replica fires its own {@code @Scheduled} methods, so without a guard two
 * backends would expire the same bookings or dispatch the same outbox events
 * twice. A Postgres transaction-level advisory lock is enough here: it needs no
 * extra table, never blocks (the {@code try} variant returns immediately), and is
 * released automatically when the surrounding transaction ends, including after a
 * crash or a lost connection.
 */
@Component
public class SchedulerGuard {
    private static final Logger log = LoggerFactory.getLogger(SchedulerGuard.class);

    private final JdbcTemplate jdbc;

    public SchedulerGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Executes {@code work} only if this instance wins the lock for {@code jobName}.
     * The work runs inside the guard's transaction, so callers do not need their own.
     */
    @Transactional
    public void runExclusively(String jobName, Runnable work) {
        Boolean acquired = jdbc.queryForObject("select pg_try_advisory_xact_lock(?)", Boolean.class, lockKey(jobName));
        if (!Boolean.TRUE.equals(acquired)) {
            log.debug("Skipping scheduled job {}: another instance holds the lock.", jobName);
            return;
        }
        work.run();
    }

    /** Stable 64-bit FNV-1a hash so every instance derives the same lock id from a job name. */
    static long lockKey(String jobName) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : jobName.getBytes(StandardCharsets.UTF_8)) {
            hash ^= (b & 0xff);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
