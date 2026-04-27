package com.cinema.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.cinema.booking.BookingDtos.BookingResponse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class PaymentDtos {
    private PaymentDtos() {
    }

    public record InitiatePaymentRequest(@NotNull UUID bookingId, String method) {
    }

    public record MockCallbackRequest(@NotBlank String paymentReference, @NotNull PaymentStatus status) {
    }

    public record PaymentResponse(UUID id, UUID bookingId, String paymentReference, BigDecimal amount, PaymentStatus status,
            String method, Instant paidAt, BookingResponse booking) {
        public static PaymentResponse from(Payment payment) {
            return new PaymentResponse(payment.getId(), payment.getBooking().getId(), payment.getPaymentReference(), payment.getAmount(),
                    payment.getStatus(), payment.getMethod(), payment.getPaidAt(), BookingResponse.from(payment.getBooking()));
        }
    }
}
