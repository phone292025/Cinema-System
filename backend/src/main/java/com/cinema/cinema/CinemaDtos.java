package com.cinema.cinema;

import java.util.UUID;

import com.cinema.hall.Hall;
import com.cinema.seat.Seat;
import com.cinema.seat.SeatType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class CinemaDtos {
    private CinemaDtos() {
    }

    public record CinemaRequest(@NotBlank String name, @NotBlank String location, @NotBlank String address, @NotBlank String city) {
    }

    public record HallRequest(@NotBlank String name, @NotBlank String type, @Min(1) int totalRows, @Min(1) int totalColumns) {
    }

    public record SeatRequest(@NotBlank String rowLabel, @Min(1) int seatNumber, @NotNull SeatType seatType) {
    }

    public record BulkSeatLayoutRequest(@Min(1) int rows, @Min(1) int columns, @NotNull SeatType defaultSeatType) {
    }

    public record CinemaResponse(UUID id, String name, String location, String address, String city) {
        public static CinemaResponse from(Cinema cinema) {
            return new CinemaResponse(cinema.getId(), cinema.getName(), cinema.getLocation(), cinema.getAddress(), cinema.getCity());
        }
    }

    public record HallResponse(UUID id, UUID cinemaId, String name, String type, int totalRows, int totalColumns) {
        public static HallResponse from(Hall hall) {
            return new HallResponse(hall.getId(), hall.getCinema().getId(), hall.getName(), hall.getType(), hall.getTotalRows(), hall.getTotalColumns());
        }
    }

    public record SeatResponse(UUID id, UUID hallId, String rowLabel, int seatNumber, SeatType seatType) {
        public static SeatResponse from(Seat seat) {
            return new SeatResponse(seat.getId(), seat.getHall().getId(), seat.getRowLabel(), seat.getSeatNumber(), seat.getSeatType());
        }
    }
}
