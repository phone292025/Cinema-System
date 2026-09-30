package com.cinema.cinema;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CinemaRepository extends JpaRepository<Cinema, UUID> {
    @Query("select count(s) > 0 from Showtime s where s.hall.cinema.id = :cinemaId")
    boolean hasShowtimes(@Param("cinemaId") UUID cinemaId);
}
