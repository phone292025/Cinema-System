package com.cinema.showtime;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cinema.audit.AuditLogService;
import com.cinema.booking.BookingStatus;
import com.cinema.common.ApiException;
import com.cinema.hall.Hall;
import com.cinema.hall.HallRepository;
import com.cinema.movie.Movie;
import com.cinema.movie.MovieRepository;
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
    static final Duration MAX_DURATION = Duration.ofHours(12);
    private static final List<BookingStatus> PAID_STATUSES = List.of(BookingStatus.PAID, BookingStatus.TICKET_ISSUED);

    private final ShowtimeRepository showtimes;
    private final ShowtimeSeatRepository showtimeSeats;
    private final MovieRepository movies;
    private final HallRepository halls;
    private final SeatRepository seats;
    private final SeatPricing pricing;
    private final AuditLogService auditLogs;

    public ShowtimeService(ShowtimeRepository showtimes, ShowtimeSeatRepository showtimeSeats, MovieRepository movies,
            HallRepository halls, SeatRepository seats, SeatPricing pricing, AuditLogService auditLogs) {
        this.showtimes = showtimes;
        this.showtimeSeats = showtimeSeats;
        this.movies = movies;
        this.halls = halls;
        this.seats = seats;
        this.pricing = pricing;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public List<ShowtimeResponse> findAllForAdmin() {
        Map<UUID, Long> totals = new HashMap<>();
        Map<UUID, Long> sold = new HashMap<>();
        for (ShowtimeSeatCount count : showtimeSeats.countSeatsByShowtimeAndStatus()) {
            totals.merge(count.showtimeId(), count.total(), Long::sum);
            if (count.status() == ShowtimeSeatStatus.BOOKED) {
                sold.merge(count.showtimeId(), count.total(), Long::sum);
            }
        }
        Map<UUID, BigDecimal> revenue = new HashMap<>();
        for (Object[] row : showtimes.sumRevenueByShowtime(PAID_STATUSES)) {
            revenue.put((UUID) row[0], (BigDecimal) row[1]);
        }
        return showtimes.findAllByOrderByStartTimeAsc().stream()
                .map(showtime -> ShowtimeResponse.from(showtime,
                        sold.getOrDefault(showtime.getId(), 0L),
                        totals.getOrDefault(showtime.getId(), 0L),
                        revenue.getOrDefault(showtime.getId(), BigDecimal.ZERO)))
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
        return showtimes.findDetailedById(id).map(ShowtimeResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Showtime not found."));
    }

    @Transactional
    public ShowtimeResponse create(ShowtimeRequest request) {
        Movie movie = movies.findById(request.movieId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Movie not found."));
        if (!request.endTime().isAfter(request.startTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A showtime must end after it starts.");
        }
        if (Duration.between(request.startTime(), request.endTime()).compareTo(MAX_DURATION) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A showtime cannot run longer than " + MAX_DURATION.toHours() + " hours.");
        }
        if (!request.startTime().isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A showtime must start in the future.");
        }
        Hall hall = halls.lockById(request.hallId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Hall not found."));
        if (showtimes.existsByHallIdAndStartTimeLessThanAndEndTimeGreaterThan(hall.getId(), request.endTime(), request.startTime())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    hall.getName() + " already has a session running at that time. Pick another slot or hall.");
        }
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setHall(hall);
        showtime.setStartTime(request.startTime());
        showtime.setEndTime(request.endTime());
        showtime.setBasePrice(request.basePrice());
        showtime.setStatus(request.status());
        showtimes.save(showtime);
        List<ShowtimeSeat> generated = seats.findByHallIdOrderByRowLabelAscSeatNumberAsc(hall.getId()).stream()
                .map(seat -> {
                    ShowtimeSeat showtimeSeat = new ShowtimeSeat();
                    showtimeSeat.setShowtime(showtime);
                    showtimeSeat.setSeat(seat);
                    showtimeSeat.setPrice(pricing.priceFor(request.basePrice(), seat.getSeatType()));
                    showtimeSeat.setStatus(ShowtimeSeatStatus.AVAILABLE);
                    return showtimeSeat;
                })
                .toList();
        showtimeSeats.saveAll(generated);
        auditLogs.recordAdmin("SHOWTIME_CREATED", "Showtime", showtime.getId().toString(),
                movie.getTitle() + " @ " + hall.getName() + " " + request.startTime());
        return ShowtimeResponse.from(showtime);
    }

    @Transactional(readOnly = true)
    public SeatAvailabilityResponse seats(UUID showtimeId) {
        if (!showtimes.existsById(showtimeId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Showtime not found.");
        }
        Instant now = Instant.now();
        List<SeatAvailability> response = showtimeSeats.findByShowtimeIdOrderBySeatRowLabelAscSeatSeatNumberAsc(showtimeId).stream()
                .map(seat -> SeatAvailability.from(seat, effectiveStatus(seat, now)))
                .toList();
        return new SeatAvailabilityResponse(showtimeId, response);
    }

    private ShowtimeSeatStatus effectiveStatus(ShowtimeSeat seat, Instant now) {
        if (seat.getStatus() == ShowtimeSeatStatus.LOCKED && seat.getLockedUntil() != null && seat.getLockedUntil().isBefore(now)) {
            return ShowtimeSeatStatus.AVAILABLE;
        }
        return seat.getStatus();
    }
}
