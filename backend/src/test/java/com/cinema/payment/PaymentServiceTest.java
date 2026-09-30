package com.cinema.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.cinema.audit.AuditLogService;
import com.cinema.auth.AuthUser;
import com.cinema.booking.Booking;
import com.cinema.booking.BookingRepository;
import com.cinema.booking.BookingService;
import com.cinema.booking.BookingStateMachine;
import com.cinema.booking.BookingStatus;
import com.cinema.cinema.Cinema;
import com.cinema.common.ApiException;
import com.cinema.common.RecordedFailureException;
import com.cinema.hall.Hall;
import com.cinema.movie.Movie;
import com.cinema.outbox.OutboxEvent;
import com.cinema.outbox.OutboxService;
import com.cinema.showtime.Showtime;
import com.cinema.user.User;
import com.cinema.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PaymentServiceTest {
    private static final String WEBHOOK_SECRET = "unit-test-webhook-secret";

    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final BookingService bookingService = mock(BookingService.class);
    private final OutboxService outbox = mock(OutboxService.class);
    private final AuditLogService auditLogs = mock(AuditLogService.class);
    private final PaymentWebhookVerifier verifier = new PaymentWebhookVerifier(WEBHOOK_SECRET);

    @Test
    void shouldTreatDuplicateSuccessfulCallbackAsIdempotent() {
        Booking booking = booking(BookingStatus.PAID, Instant.now().plusSeconds(300));
        Payment payment = payment(booking, PaymentStatus.SUCCEEDED);

        PaymentDtos.PaymentResponse response = service(true).mockCallback(AuthUser.from(booking.getUser()),
                new PaymentDtos.MockCallbackRequest("PAY-123", PaymentStatus.SUCCEEDED));

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(bookingService, never()).markSeatsBooked(booking);
    }

    @Test
    void shouldRejectMockCallbackForAnotherUsersBooking() {
        Booking booking = booking(BookingStatus.PAYMENT_PENDING, Instant.now().plusSeconds(300));
        payment(booking, PaymentStatus.PROCESSING);
        AuthUser attacker = new AuthUser(UUID.randomUUID(), "Other", "other@example.com", UserRole.CUSTOMER);

        assertThatThrownBy(() -> service(true).mockCallback(attacker, new PaymentDtos.MockCallbackRequest("PAY-123", PaymentStatus.SUCCEEDED)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Booking does not belong to this user.");
        verify(bookingService, never()).markSeatsBooked(booking);
    }

    @Test
    void mockCallbackIsNotFoundWhenTheMockGatewayIsDisabled() {
        Booking booking = booking(BookingStatus.PAYMENT_PENDING, Instant.now().plusSeconds(300));
        payment(booking, PaymentStatus.PROCESSING);

        assertThatThrownBy(() -> service(false).mockCallback(AuthUser.from(booking.getUser()),
                new PaymentDtos.MockCallbackRequest("PAY-123", PaymentStatus.SUCCEEDED)))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void successfulCallbackBooksSeatsAndQueuesTicketIssue() {
        Booking booking = booking(BookingStatus.PAYMENT_PENDING, Instant.now().plusSeconds(300));
        Payment payment = payment(booking, PaymentStatus.PROCESSING);

        service(true).mockCallback(AuthUser.from(booking.getUser()), new PaymentDtos.MockCallbackRequest("PAY-123", PaymentStatus.SUCCEEDED));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAID);
        verify(bookingService).markSeatsBooked(booking);
        verify(outbox).enqueue(eq(OutboxEvent.BOOKING_PAID), eq(booking.getId()), anyMap());
    }

    @Test
    void callbackAfterTheHoldExpiredRecordsTheFailureInsteadOfRollingItBack() {
        Booking booking = booking(BookingStatus.PAYMENT_PENDING, Instant.now().minusSeconds(60));
        Payment payment = payment(booking, PaymentStatus.PROCESSING);

        assertThatThrownBy(() -> service(true).mockCallback(AuthUser.from(booking.getUser()),
                new PaymentDtos.MockCallbackRequest("PAY-123", PaymentStatus.SUCCEEDED)))
                .isInstanceOf(RecordedFailureException.class);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(bookingService).expire(booking);
        verify(bookingService, never()).markSeatsBooked(any());
    }

    @Test
    void signedWebhookAppliesTheGatewayResult() {
        Booking booking = booking(BookingStatus.PAYMENT_PENDING, Instant.now().plusSeconds(300));
        Payment payment = payment(booking, PaymentStatus.PROCESSING);
        byte[] body = "{\"paymentReference\":\"PAY-123\",\"status\":\"SUCCEEDED\"}".getBytes(StandardCharsets.UTF_8);

        PaymentDtos.WebhookAcknowledgement ack = service(true).webhook(body, "sha256=" + verifier.sign(body));

        assertThat(ack.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void webhookWithABadSignatureChangesNothing() {
        Booking booking = booking(BookingStatus.PAYMENT_PENDING, Instant.now().plusSeconds(300));
        Payment payment = payment(booking, PaymentStatus.PROCESSING);
        byte[] body = "{\"paymentReference\":\"PAY-123\",\"status\":\"SUCCEEDED\"}".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service(true).webhook(body, "sha256=" + "0".repeat(64)))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    }

    private PaymentService service(boolean mockGatewayEnabled) {
        return new PaymentService(payments, bookings, bookingService, new BookingStateMachine(), outbox, auditLogs, verifier,
                new ObjectMapper(), mockGatewayEnabled);
    }

    private Payment payment(Booking booking, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setBooking(booking);
        payment.setPaymentReference("PAY-123");
        payment.setAmount(new BigDecimal("42.00"));
        payment.setMethod("MOCK");
        payment.setStatus(status);
        when(payments.findByPaymentReference("PAY-123")).thenReturn(Optional.of(payment));
        when(bookings.lockById(booking.getId())).thenReturn(Optional.of(booking));
        return payment;
    }

    private Booking booking(BookingStatus status, Instant expiresAt) {
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
        booking.setStatus(status);
        booking.setExpiresAt(expiresAt);
        return booking;
    }
}
