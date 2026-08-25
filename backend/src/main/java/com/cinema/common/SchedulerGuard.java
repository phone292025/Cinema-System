package com.cinema.common;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SchedulerGuard {
    private static final Logger log = LoggerFactory.getLogger(SchedulerGuard.class);

    private final JdbcTemplate jdbc;

    public SchedulerGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void runExclusively(String jobName, Runnable work) {
        Boolean acquired = jdbc.queryForObject("select pg_try_advisory_xact_lock(?)", Boolean.class, lockKey(jobName));
        if (!Boolean.TRUE.equals(acquired)) {
            log.debug("Skipping scheduled job {}: another instance holds the lock.", jobName);
            return;
        }
        work.run();
    }

    static long lockKey(String jobName) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : jobName.getBytes(StandardCharsets.UTF_8)) {
            hash ^= (b & 0xff);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
