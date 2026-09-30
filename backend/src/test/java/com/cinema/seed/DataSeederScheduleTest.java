package com.cinema.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.cinema.showtime.Showtime;

import org.junit.jupiter.api.Test;

class DataSeederScheduleTest {
    private final Instant now = Instant.parse("2026-09-30T10:05:00Z");
    private final Duration film = Duration.ofMinutes(120);

    @Test
    void startsAtTheNextQuarterHourWhenTheHallIsFree() {
        assertThat(DataSeeder.firstFreeSlot(List.of(), now, film)).isEqualTo(Instant.parse("2026-09-30T10:15:00Z"));
    }

    @Test
    void aSessionFarInTheFutureDoesNotPushTheScheduleOut() {
        Showtime premiere = showtime("2027-03-01T18:00:00Z", "2027-03-01T20:22:00Z");

        assertThat(DataSeeder.firstFreeSlot(List.of(premiere), now, film)).isEqualTo(Instant.parse("2026-09-30T10:15:00Z"));
    }

    @Test
    void skipsPastABusySlotWithATurnaroundGap() {
        Showtime busy = showtime("2026-09-30T11:00:00Z", "2026-09-30T13:00:00Z");

        assertThat(DataSeeder.firstFreeSlot(List.of(busy), now, film)).isEqualTo(Instant.parse("2026-09-30T13:30:00Z"));
    }

    private Showtime showtime(String start, String end) {
        Showtime showtime = new Showtime();
        showtime.setStartTime(Instant.parse(start));
        showtime.setEndTime(Instant.parse(end));
        return showtime;
    }
}
