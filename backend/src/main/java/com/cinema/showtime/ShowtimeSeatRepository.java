package com.cinema.showtime;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, UUID> {
    List<ShowtimeSeat> findByShowtimeIdOrderBySeatRowLabelAscSeatSeatNumberAsc(UUID showtimeId);

    List<ShowtimeSeat> findByStatusAndLockedUntilBefore(ShowtimeSeatStatus status, Instant now);

    long countByStatus(ShowtimeSeatStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ss from ShowtimeSeat ss join fetch ss.seat where ss.showtime.id = :showtimeId and ss.seat.id in :seatIds")
    List<ShowtimeSeat> lockByShowtimeAndSeatIds(@Param("showtimeId") UUID showtimeId, @Param("seatIds") List<UUID> seatIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ss from ShowtimeSeat ss where ss.showtime.id = :showtimeId and ss.seat.id = :seatId")
    Optional<ShowtimeSeat> lockOne(@Param("showtimeId") UUID showtimeId, @Param("seatId") UUID seatId);
}
