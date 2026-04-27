package com.cinema.payment;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.cinema.auth.AuthUser;
import com.cinema.booking.Booking;
import com.cinema.booking.BookingRepository;
import com.cinema.booking.BookingService;
import com.cinema.booking.BookingStatus;
import com.cinema.common.ApiException;
import com.cinema.payment.PaymentDtos.InitiatePaymentRequest;
import com.cinema.payment.PaymentDtos.MockCallbackRequest;
import com.cinema.payment.PaymentDtos.PaymentResponse;
import com.cinema.user.UserRole;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final BookingRepository bookings;
    private final BookingService bookingService;

    public PaymentService(PaymentRepository payments, BookingRepository bookings, BookingService bookingService) {
        this.payments = payments;
        this.bookings = bookings;
        this.bookingService = bookingService;
    }

    @Transactional
    public PaymentResponse initiate(AuthUser user, InitiatePaymentRequest request) {
        Booking booking = bookings.lockById(request.bookingId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found."));
        assertOwnsOrAdmin(user, booking);
        if (booking.getStatus() == BookingStatus.PAID) {
            return payments.findFirstByBookingIdAndStatus(booking.getId(), PaymentStatus.SUCCEEDED).map(PaymentResponse::from)
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Booking is paid but payment record is missing."));
        }
        if (booking.getExpiresAt() == null || booking.getExpiresAt().isBefore(Instant.now())) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingService.releaseSeats(booking);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Booking lock expired. Please select seats again.");
        }
        if (booking.getStatus() != BookingStatus.LOCKED && booking.getStatus() != BookingStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment cannot be initiated for booking status " + booking.getStatus());
        }
        Payment payment = payments.findFirstByBookingIdAndStatus(booking.getId(), PaymentStatus.PENDING).orElseGet(() -> {
            Payment created = new Payment();
            created.setBooking(booking);
            created.setPaymentReference("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
            created.setAmount(booking.getTotalAmount());
            created.setMethod(request.method() == null || request.method().isBlank() ? "MOCK" : request.method());
            created.setStatus(PaymentStatus.PENDING);
            return payments.save(created);
        });
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse mockCallback(MockCallbackRequest request) {
        Payment payment = payments.findByPaymentReference(request.paymentReference())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment reference not found."));
        Booking booking = bookings.lockById(payment.getBooking().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found."));
        payment.setBooking(booking);

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return PaymentResponse.from(payment);
        }
        if (request.status() == PaymentStatus.FAILED) {
            payment.setStatus(PaymentStatus.FAILED);
            return PaymentResponse.from(payment);
        }
        if (request.status() != PaymentStatus.SUCCEEDED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mock callback only accepts SUCCEEDED or FAILED.");
        }
        if (booking.getExpiresAt() == null || booking.getExpiresAt().isBefore(Instant.now())) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingService.releaseSeats(booking);
            payment.setStatus(PaymentStatus.FAILED);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Booking lock expired before payment succeeded.");
        }
        if (booking.getStatus() == BookingStatus.PAID) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setPaidAt(payment.getPaidAt() == null ? Instant.now() : payment.getPaidAt());
            return PaymentResponse.from(payment);
        }
        if (booking.getStatus() != BookingStatus.LOCKED && booking.getStatus() != BookingStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot pay booking with status " + booking.getStatus());
        }
        bookingService.markSeatsBooked(booking);
        booking.setStatus(BookingStatus.PAID);
        booking.setUpdatedAt(Instant.now());
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAt(Instant.now());
        return PaymentResponse.from(payment);
    }

    private void assertOwnsOrAdmin(AuthUser authUser, Booking booking) {
        if (!booking.getUser().getId().equals(authUser.id()) && authUser.role() != UserRole.ADMIN && authUser.role() != UserRole.STAFF) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Booking does not belong to this user.");
        }
    }
}
