package com.cinema.cinema;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CinemaRepository extends JpaRepository<Cinema, UUID> {
}
