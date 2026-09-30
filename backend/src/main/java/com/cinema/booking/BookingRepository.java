package com.cinema.booking;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    @EntityGraph(attributePaths = { "showtime.movie", "showtime.hall.cinema", "items.seat" })
    List<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = { "showtime.movie", "showtime.hall.cinema", "items.seat" })
    Optional<Booking> findDetailedById(UUID id);

    @EntityGraph(attributePaths = { "showtime.movie", "showtime.hall.cinema", "items.seat" })
    Optional<Booking> findByBookingCodeIgnoreCase(String bookingCode);

    List<Booking> findByStatusInAndExpiresAtBefore(List<BookingStatus> statuses, Instant now);

    boolean existsByBookingCode(String bookingCode);

    long countByStatusIn(Collection<BookingStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct b from Booking b left join fetch b.items where b.id = :id")
    Optional<Booking> lockById(@Param("id") UUID id);
}
