package com.cinema.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.cinema.audit.AuditLogService;
import com.cinema.booking.Booking;
import com.cinema.booking.BookingRepository;
import com.cinema.booking.BookingService;
import com.cinema.booking.BookingStateMachine;
import com.cinema.booking.BookingStatus;
import com.cinema.cinema.Cinema;
import com.cinema.hall.Hall;
import com.cinema.movie.Movie;
import com.cinema.outbox.OutboxService;
import com.cinema.showtime.Showtime;
import com.cinema.user.User;
import com.cinema.user.UserRole;

import org.junit.jupiter.api.Test;

class PaymentServiceTest {
    @Test
    void shouldTreatDuplicateSuccessfulCallbackAsIdempotent() {
        PaymentRepository payments = mock(PaymentRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        BookingService bookingService = mock(BookingService.class);
        OutboxService outbox = mock(OutboxService.class);
        AuditLogService auditLogs = mock(AuditLogService.class);
        Booking booking = paidBooking();
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setBooking(booking);
        payment.setPaymentReference("PAY-123");
        payment.setAmount(new BigDecimal("42.00"));
        payment.setMethod("MOCK");
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAt(Instant.now());

        when(payments.findByPaymentReference("PAY-123")).thenReturn(Optional.of(payment));
        when(bookings.lockById(booking.getId())).thenReturn(Optional.of(booking));

        PaymentService service = new PaymentService(payments, bookings, bookingService, new BookingStateMachine(), outbox, auditLogs);
        PaymentDtos.PaymentResponse response = service.mockCallback(new PaymentDtos.MockCallbackRequest("PAY-123", PaymentStatus.SUCCEEDED));

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(bookingService, never()).markSeatsBooked(booking);
    }

    private Booking paidBooking() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(UserRole.CUSTOMER);

        Movie movie = new Movie();
        movie.setId(UUID.randomUUID());
        movie.setTitle("Aurora Run");

        Cinema cinema = new Cinema();
        cinema.setId(UUID.randomUUID());
        cinema.setName("Central Cineplex");

        Hall hall = new Hall();
        hall.setId(UUID.randomUUID());
        hall.setName("Hall 1");
        hall.setCinema(cinema);

        Showtime showtime = new Showtime();
        showtime.setId(UUID.randomUUID());
        showtime.setMovie(movie);
        showtime.setHall(hall);
        showtime.setStartTime(Instant.now().plusSeconds(3600));

        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setBookingCode("CBX-TEST");
        booking.setTotalAmount(new BigDecimal("42.00"));
        booking.setStatus(BookingStatus.PAID);
        return booking;
    }
}
