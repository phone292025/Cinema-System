package com.cinema.showtime;

import java.util.UUID;

/** Aggregated seat counts per showtime, so admin lists avoid one query per session. */
public record ShowtimeSeatCount(UUID showtimeId, ShowtimeSeatStatus status, long total) {
}
