package com.cinema.seat;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, UUID> {
    List<Seat> findByHallIdOrderByRowLabelAscSeatNumberAsc(UUID hallId);
}
