package com.cinema.movie;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieRepository extends JpaRepository<Movie, UUID> {
    List<Movie> findByStatusInOrderByImdbRatingDescTitleAsc(List<MovieStatus> statuses);

    Optional<Movie> findByTitleIgnoreCase(String title);

    Optional<Movie> findFirstBySlugOrderByReleaseDateDesc(String slug);

    @Query("select count(s) > 0 from Showtime s where s.movie.id = :movieId")
    boolean hasShowtimes(@Param("movieId") UUID movieId);
}
