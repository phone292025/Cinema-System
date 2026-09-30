package com.cinema.booking;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cinema.seat.SeatType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public final class BookingDtos {
    private BookingDtos() {
    }

    public record LockSeatsRequest(@NotNull UUID showtimeId, @NotEmpty List<UUID> seatIds) {
    }

    public record BookingItemResponse(UUID seatId, String rowLabel, int seatNumber, SeatType seatType, BigDecimal price) {
    }

    public record BookingResponse(UUID id, String bookingCode, UUID showtimeId, String movieTitle, String cinemaName, String hallName,
            Instant startTime, BigDecimal totalAmount, BookingStatus status, Instant expiresAt, List<BookingItemResponse> seats,
            Instant cancellableUntil) {
        public static BookingResponse from(Booking booking) {
            return from(booking, null);
        }

        /** {@code paidCancelCutoff} is how long before the show a paid booking stops being cancellable; null leaves it unknown. */
        public static BookingResponse from(Booking booking, Duration paidCancelCutoff) {
            Instant startTime = booking.getShowtime().getStartTime();
            return new BookingResponse(booking.getId(), booking.getBookingCode(), booking.getShowtime().getId(),
                    booking.getShowtime().getMovie().getTitle(), booking.getShowtime().getHall().getCinema().getName(),
                    booking.getShowtime().getHall().getName(), startTime, booking.getTotalAmount(),
                    booking.getStatus(), booking.getExpiresAt(), booking.getItems().stream().map(item -> new BookingItemResponse(
                            item.getSeat().getId(), item.getSeat().getRowLabel(), item.getSeat().getSeatNumber(), item.getSeat().getSeatType(),
                            item.getPrice()))
                            .toList(),
                    cancellableUntil(booking.getStatus(), startTime, paidCancelCutoff));
        }

        private static Instant cancellableUntil(BookingStatus status, Instant startTime, Duration paidCancelCutoff) {
            return switch (status) {
                case LOCKED, PAYMENT_PENDING -> startTime;
                case PAID, TICKET_ISSUED -> paidCancelCutoff == null ? null : startTime.minus(paidCancelCutoff);
                default -> null;
            };
        }
    }
}
