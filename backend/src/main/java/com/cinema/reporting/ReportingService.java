package com.cinema.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cinema.booking.BookingItemRepository;
import com.cinema.booking.BookingRepository;
import com.cinema.booking.BookingStatus;
import com.cinema.movie.MovieRepository;
import com.cinema.payment.PaymentRepository;
import com.cinema.payment.PaymentStatus;
import com.cinema.reporting.ReportingDtos.DashboardResponse;
import com.cinema.reporting.ReportingDtos.OccupancyResponse;
import com.cinema.reporting.ReportingDtos.RevenuePoint;
import com.cinema.reporting.ReportingDtos.TopMovieResponse;
import com.cinema.showtime.ShowtimeRepository;
import com.cinema.showtime.ShowtimeSeatRepository;
import com.cinema.showtime.ShowtimeSeatStatus;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportingService {
    private static final List<BookingStatus> PAID_STATUSES = List.of(BookingStatus.PAID, BookingStatus.TICKET_ISSUED);
    private static final int REVENUE_DAYS = 7;
    private static final int TOP_MOVIES = 5;

    private final BookingRepository bookings;
    private final BookingItemRepository bookingItems;
    private final PaymentRepository payments;
    private final MovieRepository movies;
    private final ShowtimeRepository showtimes;
    private final ShowtimeSeatRepository showtimeSeats;
    private final ZoneId zone;

    public ReportingService(BookingRepository bookings, BookingItemRepository bookingItems, PaymentRepository payments,
            MovieRepository movies, ShowtimeRepository showtimes, ShowtimeSeatRepository showtimeSeats, ZoneId businessZone) {
        this.bookings = bookings;
        this.bookingItems = bookingItems;
        this.payments = payments;
        this.movies = movies;
        this.showtimes = showtimes;
        this.showtimeSeats = showtimeSeats;
        this.zone = businessZone;
    }

    public DashboardResponse summary() {
        OccupancyResponse occupancy = occupancy();
        return new DashboardResponse(movies.count(), showtimes.count(), bookings.count(), bookings.countByStatusIn(PAID_STATUSES),
                payments.countByStatus(PaymentStatus.FAILED), payments.sumAmountByStatus(PaymentStatus.SUCCEEDED),
                occupancy.occupancyRate());
    }

    public List<RevenuePoint> revenue() {
        LocalDate today = LocalDate.now(zone);
        LocalDate firstDay = today.minusDays(REVENUE_DAYS - 1L);
        Map<LocalDate, BigDecimal> revenueByDay = new LinkedHashMap<>();
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            revenueByDay.put(day, BigDecimal.ZERO);
        }
        for (Object[] row : payments.sumSucceededByDay(zone.getId(), firstDay.atStartOfDay(zone).toInstant())) {
            LocalDate day = LocalDate.parse((String) row[0]);
            revenueByDay.computeIfPresent(day, (ignored, total) -> total.add((BigDecimal) row[1]));
        }
        return revenueByDay.entrySet().stream().map(entry -> new RevenuePoint(entry.getKey(), entry.getValue())).toList();
    }

    public OccupancyResponse occupancy() {
        long bookedSeats = showtimeSeats.countByStatus(ShowtimeSeatStatus.BOOKED);
        long lockedSeats = showtimeSeats.countByStatus(ShowtimeSeatStatus.LOCKED);
        long totalSeats = showtimeSeats.count();
        double rate = totalSeats == 0 ? 0 : (bookedSeats * 100.0) / totalSeats;
        return new OccupancyResponse(totalSeats, bookedSeats, lockedSeats, rate);
    }

    public List<TopMovieResponse> topMovies() {
        return bookingItems.countSeatsByMovie(PAID_STATUSES, PageRequest.of(0, TOP_MOVIES)).stream()
                .map(row -> new TopMovieResponse((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }
}
