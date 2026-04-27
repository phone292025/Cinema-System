package com.cinema.booking;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingItemRepository extends JpaRepository<BookingItem, UUID> {
}
