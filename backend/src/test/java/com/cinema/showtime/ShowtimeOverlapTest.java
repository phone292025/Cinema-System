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

import com.cinema.audit.AuditLogService;
import com.cinema.cinema.Cinema;
import com.cinema.common.ApiException;
import com.cinema.hall.Hall;
import com.cinema.hall.HallRepository;
import com.cinema.movie.Movie;
import com.cinema.movie.MovieRepository;
import com.cinema.seat.Seat;
import com.cinema.seat.SeatRepository;
import com.cinema.seat.SeatType;
import com.cinema.showtime.ShowtimeDtos.ShowtimeRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

class ShowtimeOverlapTest {
    private final ShowtimeRepository showtimes = mock(ShowtimeRepository.class);
    private final ShowtimeSeatRepository showtimeSeats = mock(ShowtimeSeatRepository.class);
    private final MovieRepository movies = mock(MovieRepository.class);
    private final HallRepository halls = mock(HallRepository.class);
    private final SeatRepository seats = mock(SeatRepository.class);
    private final ShowtimeService service = new ShowtimeService(showtimes, showtimeSeats, movies, halls, seats,
            new SeatPricing(new BigDecimal("4.00")), mock(AuditLogService.class));

    private final UUID movieId = UUID.randomUUID();
    private final UUID hallId = UUID.randomUUID();
    private final Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS).plus(2, ChronoUnit.DAYS);
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
        when(halls.lockById(hallId)).thenReturn(Optional.of(hall));
        when(seats.findByHallIdOrderByRowLabelAscSeatNumberAsc(any())).thenReturn(List.of());
        when(showtimes.save(any())).thenAnswer(invocation -> {
            Showtime showtime = invocation.getArgument(0);
            showtime.setId(UUID.randomUUID());
            return showtime;
        });
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

    @Test
    void rejectsASessionInThePast() {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> service.create(request(past, past.plus(2, ChronoUnit.HOURS))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must start in the future");
    }

    @Test
    void rejectsAnImplausiblyLongSession() {
        assertThatThrownBy(() -> service.create(request(start, start.plus(13, ChronoUnit.HOURS))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot run longer than 12 hours");
    }

    @Test
    @SuppressWarnings("unchecked")
    void pricesPremiumSeatsWithTheSurcharge() {
        Seat regular = new Seat();
        regular.setSeatType(SeatType.REGULAR);
        Seat premium = new Seat();
        premium.setSeatType(SeatType.PREMIUM);
        when(seats.findByHallIdOrderByRowLabelAscSeatNumberAsc(any())).thenReturn(List.of(regular, premium));
        ArgumentCaptor<List<ShowtimeSeat>> saved = ArgumentCaptor.forClass(List.class);

        service.create(request(start, end));

        verify(showtimeSeats).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(ShowtimeSeat::getPrice)
                .containsExactly(new BigDecimal("15.00"), new BigDecimal("19.00"));
    }

    private ShowtimeRequest request(Instant from, Instant to) {
        return new ShowtimeRequest(movieId, hallId, from, to, new BigDecimal("15.00"), ShowtimeStatus.SCHEDULED);
    }
}
