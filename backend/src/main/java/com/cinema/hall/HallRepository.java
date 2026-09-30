package com.cinema.hall;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HallRepository extends JpaRepository<Hall, UUID> {
    List<Hall> findByCinemaId(UUID cinemaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Hall h join fetch h.cinema where h.id = :id")
    Optional<Hall> lockById(@Param("id") UUID id);
}
