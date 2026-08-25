package com.cinema.showtime;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowtimeRepository extends JpaRepository<Showtime, UUID> {
    List<Showtime> findByMovieIdAndStartTimeAfterOrderByStartTimeAsc(UUID movieId, Instant startTime);

    List<Showtime> findByHallCinemaIdAndStartTimeAfterOrderByStartTimeAsc(UUID cinemaId, Instant startTime);

    List<Showtime> findByStartTimeBetweenOrderByStartTimeAsc(Instant from, Instant to);

    /**
     * A hall can only run one film at a time, so a new session may not straddle an
     * existing one. Intervals overlap when each starts before the other ends.
     */
    boolean existsByHallIdAndStartTimeLessThanAndEndTimeGreaterThan(UUID hallId, Instant newEndTime, Instant newStartTime);
}
