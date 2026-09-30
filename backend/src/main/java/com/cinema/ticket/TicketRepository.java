package com.cinema.ticket;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.cinema.booking.BookingStatus;
import com.cinema.outbox.OutboxStatus;
import com.cinema.user.User;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    Optional<Ticket> findByBookingId(UUID bookingId);

    @EntityGraph(attributePaths = { "booking.showtime.movie", "booking.showtime.hall.cinema", "booking.items.seat" })
    Optional<Ticket> findDetailedByBookingId(UUID bookingId);

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

    @Query("""
            select b.id from Booking b
            where b.status in :statuses
              and not exists (select t.id from Ticket t where t.booking = b)
              and not exists (
                  select e.id from OutboxEvent e
                  where e.aggregateId = b.id and e.eventType = :eventType and e.status in :openStatuses)
            """)
    List<UUID> findBookingsMissingTicket(@Param("statuses") Collection<BookingStatus> statuses, @Param("eventType") String eventType,
            @Param("openStatuses") Collection<OutboxStatus> openStatuses);
}
