package com.cinema.cinema;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HallRepository extends JpaRepository<Hall, UUID> {
    List<Hall> findByCinemaId(UUID cinemaId);
}
