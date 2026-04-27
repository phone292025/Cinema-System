package com.cinema.movie;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, UUID> {
    List<Movie> findByStatusInOrderByReleaseDateDesc(List<MovieStatus> statuses);

    List<Movie> findByStatusInOrderByImdbRatingDescTitleAsc(List<MovieStatus> statuses);

    Optional<Movie> findByTitleIgnoreCase(String title);
}
