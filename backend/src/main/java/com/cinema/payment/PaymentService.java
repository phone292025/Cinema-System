package com.cinema.payment;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.cinema.audit.AuditLogService;
import com.cinema.auth.AuthUser;
import com.cinema.booking.Booking;
import com.cinema.booking.BookingAccess;
import com.cinema.booking.BookingEvent;
import com.cinema.booking.BookingRepository;
import com.cinema.booking.BookingService;
import com.cinema.booking.BookingStateMachine;
import com.cinema.booking.BookingStatus;
import com.cinema.common.ApiException;
import com.cinema.common.RecordedFailureException;
import com.cinema.outbox.OutboxEvent;
import com.cinema.outbox.OutboxService;
import com.cinema.payment.PaymentDtos.InitiatePaymentRequest;
import com.cinema.payment.PaymentDtos.MockCallbackRequest;
import com.cinema.payment.PaymentDtos.PaymentResponse;
import com.cinema.payment.PaymentDtos.WebhookAcknowledgement;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final List<BookingStatus> PAYABLE = List.of(BookingStatus.LOCKED, BookingStatus.PAYMENT_PENDING);

    private final PaymentRepository payments;
    private final BookingRepository bookings;
    private final BookingService bookingService;
    private final BookingStateMachine stateMachine;
    private final OutboxService outbox;
    private final AuditLogService auditLogs;
    private final PaymentWebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;
    private final boolean mockGatewayEnabled;

    public PaymentService(PaymentRepository payments, BookingRepository bookings, BookingService bookingService,
            BookingStateMachine stateMachine, OutboxService outbox, AuditLogService auditLogs, PaymentWebhookVerifier webhookVerifier,
            ObjectMapper objectMapper, @Value("${app.payments.mock-gateway-enabled:true}") boolean mockGatewayEnabled) {
        this.payments = payments;
        this.bookings = bookings;
        this.bookingService = bookingService;
        this.stateMachine = stateMachine;
        this.outbox = outbox;
        this.auditLogs = auditLogs;
        this.webhookVerifier = webhookVerifier;
        this.objectMapper = objectMapper;
        this.mockGatewayEnabled = mockGatewayEnabled;
        if (mockGatewayEnabled) {
            log.warn("The mock payment gateway is enabled: customers can confirm their own payments. Disable it in production.");
        }
    }

    @Transactional(noRollbackFor = RecordedFailureException.class)
    public PaymentResponse initiate(AuthUser user, InitiatePaymentRequest request) {
        Booking booking = bookings.lockById(request.bookingId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found."));
        BookingAccess.requireOwnerOrAdmin(user, booking);
        if (booking.getStatus() == BookingStatus.PAID || booking.getStatus() == BookingStatus.TICKET_ISSUED) {
            return payments.findFirstByBookingIdAndStatus(booking.getId(), PaymentStatus.SUCCEEDED).map(PaymentResponse::from)
                    .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Booking is paid but payment record is missing."));
        }
        if (!PAYABLE.contains(booking.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment cannot be initiated for booking status " + booking.getStatus());
        }
        if (isExpired(booking)) {
            bookingService.expire(booking);
            throw new RecordedFailureException(HttpStatus.BAD_REQUEST, "Booking lock expired. Please select seats again.");
        }
        if (booking.getStatus() == BookingStatus.LOCKED) {
            booking.setStatus(stateMachine.transition(booking.getStatus(), BookingEvent.PAYMENT_STARTED));
            booking.setUpdatedAt(Instant.now());
        }
        Payment payment = payments.findFirstByBookingIdAndStatusIn(booking.getId(), List.of(PaymentStatus.PENDING, PaymentStatus.PROCESSING))
                .orElseGet(() -> {
                    Payment created = new Payment();
                    created.setBooking(booking);
                    created.setPaymentReference("PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT));
                    created.setAmount(booking.getTotalAmount());
                    created.setMethod(request.method() == null || request.method().isBlank() ? "MOCK" : request.method().trim());
                    created.setStatus(PaymentStatus.PROCESSING);
                    return payments.save(created);
                });
        return PaymentResponse.from(payment);
    }

    @Transactional(noRollbackFor = RecordedFailureException.class)
    public PaymentResponse mockCallback(AuthUser user, MockCallbackRequest request) {
        if (!mockGatewayEnabled) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Test payments are turned off on this server.");
        }
        Payment payment = applyGatewayResult(request.paymentReference(), request.status(),
                booking -> BookingAccess.requireOwnerOrAdmin(user, booking));
        return PaymentResponse.from(payment);
    }

    @Transactional(noRollbackFor = RecordedFailureException.class)
    public WebhookAcknowledgement webhook(byte[] body, String signature) {
        webhookVerifier.verify(body, signature);
        MockCallbackRequest request = parseWebhook(body);
        Payment payment = applyGatewayResult(request.paymentReference(), request.status(), booking -> {
        });
        return new WebhookAcknowledgement(payment.getPaymentReference(), payment.getStatus());
    }

    private Payment applyGatewayResult(String paymentReference, PaymentStatus result, Consumer<Booking> accessCheck) {
        Payment payment = payments.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment reference not found."));
        Booking booking = bookings.lockById(payment.getBooking().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found."));
        accessCheck.accept(booking);
        payment.setBooking(booking);
        auditLogs.system("PAYMENT_CALLBACK_RECEIVED", "Payment", payment.getId().toString(), result.name());

        if (payment.getStatus() == PaymentStatus.SUCCEEDED || payment.getStatus() == PaymentStatus.REFUNDED) {
            return payment;
        }
        if (payment.getStatus() == PaymentStatus.FAILED) {
            if (result == PaymentStatus.FAILED) {
                return payment;
            }
            throw new ApiException(HttpStatus.CONFLICT, "Payment has already failed.");
        }
        if (result == PaymentStatus.FAILED) {
            payment.setStatus(PaymentStatus.FAILED);
            if (stateMachine.canTransition(booking.getStatus(), BookingEvent.PAYMENT_FAILED)) {
                booking.setStatus(stateMachine.transition(booking.getStatus(), BookingEvent.PAYMENT_FAILED));
                booking.setUpdatedAt(Instant.now());
            }
            bookingService.releaseSeats(booking);
            return payment;
        }
        if (result != PaymentStatus.SUCCEEDED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A payment result must be SUCCEEDED or FAILED.");
        }
        if (booking.getStatus() == BookingStatus.PAID || booking.getStatus() == BookingStatus.TICKET_ISSUED) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setPaidAt(payment.getPaidAt() == null ? Instant.now() : payment.getPaidAt());
            return payment;
        }
        if (!PAYABLE.contains(booking.getStatus())) {
            payment.setStatus(PaymentStatus.FAILED);
            throw new RecordedFailureException(HttpStatus.CONFLICT,
                    "Booking is no longer awaiting payment (status " + booking.getStatus() + ").");
        }
        if (isExpired(booking)) {
            bookingService.expire(booking);
            payment.setStatus(PaymentStatus.FAILED);
            throw new RecordedFailureException(HttpStatus.BAD_REQUEST, "Booking lock expired before payment succeeded.");
        }
        if (booking.getStatus() == BookingStatus.LOCKED) {
            booking.setStatus(stateMachine.transition(booking.getStatus(), BookingEvent.PAYMENT_STARTED));
        }
        bookingService.markSeatsBooked(booking);
        booking.setStatus(stateMachine.transition(booking.getStatus(), BookingEvent.PAYMENT_SUCCEEDED));
        booking.setUpdatedAt(Instant.now());
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAt(Instant.now());
        auditLogs.system("BOOKING_PAID", "Booking", booking.getId().toString(), payment.getPaymentReference());
        outbox.enqueue(OutboxEvent.BOOKING_PAID, booking.getId(),
                Map.of("bookingId", booking.getId().toString(), "paymentId", payment.getId().toString()));
        return payment;
    }

    private boolean isExpired(Booking booking) {
        return booking.getExpiresAt() == null || booking.getExpiresAt().isBefore(Instant.now());
    }

    private MockCallbackRequest parseWebhook(byte[] body) {
        try {
            MockCallbackRequest request = objectMapper.readValue(body, MockCallbackRequest.class);
            if (request.paymentReference() == null || request.paymentReference().isBlank() || request.status() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Webhook body needs paymentReference and status.");
            }
            return request;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Webhook body is not valid JSON.");
        }
    }
}
