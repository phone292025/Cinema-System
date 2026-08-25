package com.cinema.showtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.cinema.cinema.Cinema;
import com.cinema.common.ApiException;
import com.cinema.hall.Hall;
import com.cinema.hall.HallRepository;
import com.cinema.movie.Movie;
import com.cinema.movie.MovieRepository;
import com.cinema.seat.SeatRepository;
import com.cinema.showtime.ShowtimeDtos.ShowtimeRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ShowtimeOverlapTest {
    private final ShowtimeRepository showtimes = mock(ShowtimeRepository.class);
    private final ShowtimeSeatRepository showtimeSeats = mock(ShowtimeSeatRepository.class);
    private final MovieRepository movies = mock(MovieRepository.class);
    private final HallRepository halls = mock(HallRepository.class);
    private final SeatRepository seats = mock(SeatRepository.class);
    private final ShowtimeService service = new ShowtimeService(showtimes, showtimeSeats, movies, halls, seats);

    private final UUID movieId = UUID.randomUUID();
    private final UUID hallId = UUID.randomUUID();
    private final Instant start = Instant.parse("2026-09-01T18:00:00Z");
    private final Instant end = start.plus(120, ChronoUnit.MINUTES);

    @BeforeEach
    void stubLookups() {
        Movie movie = new Movie();
        movie.setTitle("Test Feature");
        Cinema cinema = new Cinema();
        cinema.setName("Central Cineplex");
        Hall hall = new Hall();
        hall.setName("Hall 1");
        hall.setCinema(cinema);
        when(movies.findById(movieId)).thenReturn(Optional.of(movie));
        when(halls.findById(hallId)).thenReturn(Optional.of(hall));
        when(seats.findByHallIdOrderByRowLabelAscSeatNumberAsc(any())).thenReturn(List.of());
    }

    @Test
    void rejectsASessionThatOverlapsAnotherInTheSameHall() {
        when(showtimes.existsByHallIdAndStartTimeLessThanAndEndTimeGreaterThan(any(), eq(end), eq(start))).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(start, end)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already has a session running")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(showtimes, never()).save(any());
    }

    @Test
    void acceptsASessionInAFreeSlot() {
        when(showtimes.existsByHallIdAndStartTimeLessThanAndEndTimeGreaterThan(any(), eq(end), eq(start))).thenReturn(false);

        assertThat(service.create(request(start, end))).isNotNull();

        verify(showtimes).save(any());
    }

    @Test
    void rejectsASessionThatEndsBeforeItStarts() {
        assertThatThrownBy(() -> service.create(request(end, start)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must end after it starts");

        verify(showtimes, never()).save(any());
    }

    private ShowtimeRequest request(Instant from, Instant to) {
        return new ShowtimeRequest(movieId, hallId, from, to, new BigDecimal("15.00"), ShowtimeStatus.SCHEDULED);
    }
}
