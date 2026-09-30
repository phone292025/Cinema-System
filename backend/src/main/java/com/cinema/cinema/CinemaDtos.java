package com.cinema.cinema;

import java.util.UUID;

import com.cinema.hall.Hall;
import com.cinema.seat.Seat;
import com.cinema.seat.SeatType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class CinemaDtos {
    public static final int MAX_ROWS = 26;
    public static final int MAX_COLUMNS = 50;

    private CinemaDtos() {
    }

    public record CinemaRequest(
            @NotBlank @Size(max = 220) String name,
            @NotBlank @Size(max = 220) String location,
            @NotBlank String address,
            @NotBlank @Size(max = 120) String city) {
    }

    public record HallRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 80) String type,
            @Min(1) @Max(MAX_ROWS) int totalRows,
            @Min(1) @Max(MAX_COLUMNS) int totalColumns,
            SeatType defaultSeatType) {
    }

    public record SeatRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z]{1,8}", message = "must be 1 to 8 letters") String rowLabel,
            @Min(1) int seatNumber,
            @NotNull SeatType seatType) {
    }

    public record BulkSeatLayoutRequest(
            @Min(1) @Max(MAX_ROWS) int rows,
            @Min(1) @Max(MAX_COLUMNS) int columns,
            @NotNull SeatType defaultSeatType) {
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
