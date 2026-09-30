package com.cinema.showtime;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.cinema.booking.BookingStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowtimeRepository extends JpaRepository<Showtime, UUID> {
    @EntityGraph(attributePaths = { "movie", "hall.cinema" })
    List<Showtime> findAllByOrderByStartTimeAsc();

    @EntityGraph(attributePaths = { "movie", "hall.cinema" })
    Optional<Showtime> findDetailedById(UUID id);

    @EntityGraph(attributePaths = { "movie", "hall.cinema" })
    List<Showtime> findByMovieIdAndStartTimeAfterOrderByStartTimeAsc(UUID movieId, Instant startTime);

    @EntityGraph(attributePaths = { "movie", "hall.cinema" })
    List<Showtime> findByHallCinemaIdAndStartTimeAfterOrderByStartTimeAsc(UUID cinemaId, Instant startTime);

    @EntityGraph(attributePaths = { "movie", "hall.cinema" })
    List<Showtime> findByStartTimeBetweenOrderByStartTimeAsc(Instant from, Instant to);

    boolean existsByHallIdAndStartTimeLessThanAndEndTimeGreaterThan(UUID hallId, Instant newEndTime, Instant newStartTime);

    List<Showtime> findByHallIdAndEndTimeAfterOrderByStartTimeAsc(UUID hallId, Instant endAfter);

    @Query("""
            select b.showtime.id, sum(bi.price)
            from BookingItem bi join bi.booking b
            where b.status in :statuses
            group by b.showtime.id
            """)
    List<Object[]> sumRevenueByShowtime(@Param("statuses") Collection<BookingStatus> statuses);
}
