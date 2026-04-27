package com.cinema.payment;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByPaymentReference(String paymentReference);

    Optional<Payment> findFirstByBookingIdAndStatus(UUID bookingId, PaymentStatus status);
}
