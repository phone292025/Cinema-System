package com.cinema.admin;

import java.math.BigDecimal;

import com.cinema.booking.BookingRepository;
import com.cinema.booking.BookingStatus;
import com.cinema.movie.MovieRepository;
import com.cinema.payment.PaymentRepository;
import com.cinema.payment.PaymentStatus;
import com.cinema.showtime.ShowtimeRepository;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class AdminDashboardController {
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final MovieRepository movies;
    private final ShowtimeRepository showtimes;

    public AdminDashboardController(BookingRepository bookings, PaymentRepository payments, MovieRepository movies, ShowtimeRepository showtimes) {
        this.bookings = bookings;
        this.payments = payments;
        this.movies = movies;
        this.showtimes = showtimes;
    }

    @GetMapping
    DashboardResponse dashboard() {
        BigDecimal revenue = payments.findAll().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCEEDED)
                .map(payment -> payment.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidBookings = bookings.findAll().stream().filter(booking -> booking.getStatus() == BookingStatus.PAID).count();
        return new DashboardResponse(movies.count(), showtimes.count(), bookings.count(), paidBookings, revenue);
    }

    public record DashboardResponse(long movies, long showtimes, long bookings, long paidBookings, BigDecimal revenue) {
    }
}
