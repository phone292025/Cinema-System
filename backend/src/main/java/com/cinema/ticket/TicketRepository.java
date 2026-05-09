package com.cinema.ticket;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.cinema.user.User;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    Optional<Ticket> findByBookingId(UUID bookingId);

    Optional<Ticket> findByQrTokenHash(String qrTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.qrTokenHash = :qrTokenHash")
    Optional<Ticket> findWithLockByQrTokenHash(@Param("qrTokenHash") String qrTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where upper(t.ticketCode) = upper(:ticketCode)")
    Optional<Ticket> findWithLockByTicketCode(@Param("ticketCode") String ticketCode);

    @Modifying
    @Query("update Ticket t set t.status = :usedStatus, t.usedAt = :usedAt, t.validatedBy = :validatedBy where t.id = :id and t.status = :issuedStatus")
    int markUsedIfIssued(@Param("id") UUID id, @Param("usedAt") Instant usedAt, @Param("validatedBy") User validatedBy,
            @Param("usedStatus") TicketStatus usedStatus, @Param("issuedStatus") TicketStatus issuedStatus);
}
