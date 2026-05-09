package com.cinema.showtime;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SeatEvent(
        SeatEventType type,
        UUID showtimeId,
        UUID seatId,
        String seatCode,
        ShowtimeSeatStatus status,
        BigDecimal price,
        Instant expiresAt) {
    public static SeatEvent from(SeatEventType type, ShowtimeSeat showtimeSeat) {
        return new SeatEvent(type, showtimeSeat.getShowtime().getId(), showtimeSeat.getSeat().getId(),
                showtimeSeat.getSeat().getRowLabel() + showtimeSeat.getSeat().getSeatNumber(), showtimeSeat.getStatus(),
                showtimeSeat.getPrice(), showtimeSeat.getLockedUntil());
    }
}
