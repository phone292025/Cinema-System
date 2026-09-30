package com.cinema.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByPaymentReference(String paymentReference);

    Optional<Payment> findFirstByBookingIdAndStatus(UUID bookingId, PaymentStatus status);

    Optional<Payment> findFirstByBookingIdAndStatusIn(UUID bookingId, Collection<PaymentStatus> statuses);

    long countByStatus(PaymentStatus status);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);

    @Query(value = """
            select to_char(p.paid_at at time zone :zone, 'YYYY-MM-DD'), sum(p.amount)
            from payments p
            where p.status = 'SUCCEEDED' and p.paid_at >= :since
            group by 1
            """, nativeQuery = true)
    List<Object[]> sumSucceededByDay(@Param("zone") String zone, @Param("since") Instant since);
}
