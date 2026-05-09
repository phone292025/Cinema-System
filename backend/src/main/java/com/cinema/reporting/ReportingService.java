package com.cinema.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportingService {
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final MovieRepository movies;
    private final ShowtimeRepository showtimes;
    private final ShowtimeSeatRepository showtimeSeats;

    public ReportingService(BookingRepository bookings, PaymentRepository payments, MovieRepository movies,
            ShowtimeRepository showtimes, ShowtimeSeatRepository showtimeSeats) {
        this.bookings = bookings;
        this.payments = payments;
        this.movies = movies;
        this.showtimes = showtimes;
        this.showtimeSeats = showtimeSeats;
    }

    public DashboardResponse summary() {
        BigDecimal revenue = payments.findAll().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCEEDED)
                .map(payment -> payment.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidBookings = bookings.findAll().stream()
                .filter(booking -> booking.getStatus() == BookingStatus.PAID || booking.getStatus() == BookingStatus.TICKET_ISSUED)
                .count();
        long failedPayments = payments.findAll().stream().filter(payment -> payment.getStatus() == PaymentStatus.FAILED).count();
        long bookedSeats = showtimeSeats.countByStatus(ShowtimeSeatStatus.BOOKED);
        long totalSeats = showtimeSeats.count();
        double occupancyRate = totalSeats == 0 ? 0 : (bookedSeats * 100.0) / totalSeats;
        return new DashboardResponse(movies.count(), showtimes.count(), bookings.count(), paidBookings, failedPayments, revenue, occupancyRate);
    }

    public List<RevenuePoint> revenue() {
        Map<LocalDate, BigDecimal> revenueByDay = new LinkedHashMap<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = 6; i >= 0; i--) {
            revenueByDay.put(today.minusDays(i), BigDecimal.ZERO);
        }
        payments.findAll().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCEEDED && payment.getPaidAt() != null)
                .forEach(payment -> {
                    LocalDate day = payment.getPaidAt().atZone(ZoneOffset.UTC).toLocalDate();
                    if (revenueByDay.containsKey(day)) {
                        revenueByDay.compute(day, (ignored, total) -> total.add(payment.getAmount()));
                    }
                });
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
        return bookings.findAll().stream()
                .filter(booking -> booking.getStatus() == BookingStatus.PAID || booking.getStatus() == BookingStatus.TICKET_ISSUED)
                .collect(Collectors.groupingBy(booking -> booking.getShowtime().getMovie().getTitle(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(entry -> new TopMovieResponse(entry.getKey(), entry.getValue()))
                .toList();
    }
}
