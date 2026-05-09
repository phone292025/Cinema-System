package com.cinema.showtime;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.cinema.common.ApiException;
import com.cinema.hall.Hall;
import com.cinema.hall.HallRepository;
import com.cinema.movie.Movie;
import com.cinema.movie.MovieRepository;
import com.cinema.seat.Seat;
import com.cinema.seat.SeatRepository;
import com.cinema.showtime.ShowtimeDtos.SeatAvailability;
import com.cinema.showtime.ShowtimeDtos.SeatAvailabilityResponse;
import com.cinema.showtime.ShowtimeDtos.ShowtimeRequest;
import com.cinema.showtime.ShowtimeDtos.ShowtimeResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShowtimeService {
    private final ShowtimeRepository showtimes;
    private final ShowtimeSeatRepository showtimeSeats;
    private final MovieRepository movies;
    private final HallRepository halls;
    private final SeatRepository seats;

    public ShowtimeService(ShowtimeRepository showtimes, ShowtimeSeatRepository showtimeSeats, MovieRepository movies,
            HallRepository halls, SeatRepository seats) {
        this.showtimes = showtimes;
        this.showtimeSeats = showtimeSeats;
        this.movies = movies;
        this.halls = halls;
        this.seats = seats;
    }

    @Transactional(readOnly = true)
    public List<ShowtimeResponse> findAllForAdmin() {
        return showtimes.findAll().stream()
                .map(ShowtimeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShowtimeResponse> findByMovie(UUID movieId) {
        return showtimes.findByMovieIdAndStartTimeAfterOrderByStartTimeAsc(movieId, Instant.now()).stream()
                .map(ShowtimeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShowtimeResponse> findByCinema(UUID cinemaId) {
        return showtimes.findByHallCinemaIdAndStartTimeAfterOrderByStartTimeAsc(cinemaId, Instant.now()).stream()
                .map(ShowtimeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShowtimeResponse get(UUID id) {
        return showtimes.findById(id).map(ShowtimeResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Showtime not found."));
    }

    @Transactional
    public ShowtimeResponse create(ShowtimeRequest request) {
        Movie movie = movies.findById(request.movieId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Movie not found."));
        Hall hall = halls.findById(request.hallId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Hall not found."));
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setHall(hall);
        showtime.setStartTime(request.startTime());
        showtime.setEndTime(request.endTime());
        showtime.setBasePrice(request.basePrice());
        showtime.setStatus(request.status());
        showtimes.save(showtime);
        List<ShowtimeSeat> generated = seats.findByHallIdOrderByRowLabelAscSeatNumberAsc(hall.getId()).stream()
                .map(seat -> newShowtimeSeat(showtime, seat, request.basePrice()))
                .toList();
        showtimeSeats.saveAll(generated);
        return ShowtimeResponse.from(showtime);
    }

    @Transactional(readOnly = true)
    public SeatAvailabilityResponse seats(UUID showtimeId) {
        List<SeatAvailability> response = showtimeSeats.findByShowtimeIdOrderBySeatRowLabelAscSeatSeatNumberAsc(showtimeId).stream()
                .sorted(Comparator.comparing((ShowtimeSeat s) -> s.getSeat().getRowLabel()).thenComparing(s -> s.getSeat().getSeatNumber()))
                .map(seat -> SeatAvailability.from(seat, effectiveStatus(seat)))
                .toList();
        return new SeatAvailabilityResponse(showtimeId, response);
    }

    @Transactional
    public void releaseExpiredSeatLocks() {
        showtimeSeats.findByStatusAndLockedUntilBefore(ShowtimeSeatStatus.LOCKED, Instant.now()).forEach(seat -> {
            seat.setStatus(ShowtimeSeatStatus.AVAILABLE);
            seat.setLockedUntil(null);
        });
    }

    private ShowtimeSeat newShowtimeSeat(Showtime showtime, Seat seat, BigDecimal price) {
        ShowtimeSeat showtimeSeat = new ShowtimeSeat();
        showtimeSeat.setShowtime(showtime);
        showtimeSeat.setSeat(seat);
        showtimeSeat.setPrice(price);
        showtimeSeat.setStatus(ShowtimeSeatStatus.AVAILABLE);
        return showtimeSeat;
    }

    private ShowtimeSeatStatus effectiveStatus(ShowtimeSeat seat) {
        if (seat.getStatus() == ShowtimeSeatStatus.LOCKED && seat.getLockedUntil() != null && seat.getLockedUntil().isBefore(Instant.now())) {
            return ShowtimeSeatStatus.AVAILABLE;
        }
        return seat.getStatus();
    }
}
