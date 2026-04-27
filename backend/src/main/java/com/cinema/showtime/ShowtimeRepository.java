package com.cinema.showtime;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowtimeRepository extends JpaRepository<Showtime, UUID> {
    List<Showtime> findByMovieIdAndStartTimeAfterOrderByStartTimeAsc(UUID movieId, Instant startTime);

    List<Showtime> findByHallCinemaIdAndStartTimeAfterOrderByStartTimeAsc(UUID cinemaId, Instant startTime);
}
