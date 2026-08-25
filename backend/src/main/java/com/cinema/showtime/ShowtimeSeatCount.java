package com.cinema.showtime;

import java.util.UUID;

public record ShowtimeSeatCount(UUID showtimeId, ShowtimeSeatStatus status, long total) {
}
