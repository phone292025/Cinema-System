package com.cinema.showtime;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cinema.seat.SeatType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public final class ShowtimeDtos {
    private ShowtimeDtos() {
    }

    public record ShowtimeRequest(
            @NotNull UUID movieId,
            @NotNull UUID hallId,
            @NotNull Instant startTime,
            @NotNull Instant endTime,
            @DecimalMin("0.00") BigDecimal basePrice,
            @NotNull ShowtimeStatus status) {
    }

    public record ShowtimeResponse(UUID id, UUID movieId, String movieTitle, UUID cinemaId, String cinemaName, UUID hallId,
            String hallName, Instant startTime, Instant endTime, BigDecimal basePrice, ShowtimeStatus status) {
        public static ShowtimeResponse from(Showtime showtime) {
            return new ShowtimeResponse(showtime.getId(), showtime.getMovie().getId(), showtime.getMovie().getTitle(),
                    showtime.getHall().getCinema().getId(), showtime.getHall().getCinema().getName(), showtime.getHall().getId(),
                    showtime.getHall().getName(), showtime.getStartTime(), showtime.getEndTime(), showtime.getBasePrice(), showtime.getStatus());
        }
    }

    public record SeatAvailabilityResponse(UUID showtimeId, List<SeatAvailability> seats) {
    }

    public record SeatAvailability(UUID seatId, String rowLabel, int seatNumber, SeatType seatType, BigDecimal price,
            ShowtimeSeatStatus status, Instant lockedUntil) {
        public static SeatAvailability from(ShowtimeSeat seat, ShowtimeSeatStatus status) {
            return new SeatAvailability(seat.getSeat().getId(), seat.getSeat().getRowLabel(), seat.getSeat().getSeatNumber(),
                    seat.getSeat().getSeatType(), seat.getPrice(), status, seat.getLockedUntil());
        }
    }
}
