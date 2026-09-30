package com.cinema.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.cinema.booking.BookingDtos.BookingResponse;
import com.cinema.cinema.Cinema;
import com.cinema.hall.Hall;
import com.cinema.movie.Movie;
import com.cinema.showtime.Showtime;

import org.junit.jupiter.api.Test;

class BookingResponseTest {
    private final Instant start = Instant.parse("2026-10-01T18:00:00Z");
    private final Duration cutoff = Duration.ofHours(2);

    @Test
    void anUnpaidHoldCanBeCancelledUntilTheShowStarts() {
        assertThat(BookingResponse.from(booking(BookingStatus.LOCKED), cutoff).cancellableUntil()).isEqualTo(start);
    }

    @Test
    void aPaidBookingClosesForCancellationAtTheCutoff() {
        assertThat(BookingResponse.from(booking(BookingStatus.TICKET_ISSUED), cutoff).cancellableUntil())
                .isEqualTo(Instant.parse("2026-10-01T16:00:00Z"));
    }

    @Test
    void finishedBookingsCannotBeCancelled() {
        assertThat(BookingResponse.from(booking(BookingStatus.REFUNDED), cutoff).cancellableUntil()).isNull();
        assertThat(BookingResponse.from(booking(BookingStatus.EXPIRED), cutoff).cancellableUntil()).isNull();
    }

    @Test
    void responsesWithoutThePolicyDoNotGuessThePaidCutoff() {
        assertThat(BookingResponse.from(booking(BookingStatus.PAID)).cancellableUntil()).isNull();
    }

    private Booking booking(BookingStatus status) {
        Cinema cinema = new Cinema();
        cinema.setName("Central Cineplex");
        Hall hall = new Hall();
        hall.setName("Hall 1");
        hall.setCinema(cinema);
        Movie movie = new Movie();
        movie.setTitle("The Godfather");
        Showtime showtime = new Showtime();
        showtime.setId(UUID.randomUUID());
        showtime.setMovie(movie);
        showtime.setHall(hall);
        showtime.setStartTime(start);
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setShowtime(showtime);
        booking.setTotalAmount(new BigDecimal("18.00"));
        booking.setStatus(status);
        return booking;
    }
}
