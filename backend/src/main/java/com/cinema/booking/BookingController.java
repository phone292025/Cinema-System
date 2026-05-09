package com.cinema.booking;

import java.util.UUID;

import com.cinema.auth.AuthUser;
import com.cinema.booking.BookingDtos.BookingResponse;
import com.cinema.booking.BookingDtos.LockSeatsRequest;
import com.cinema.idempotency.Idempotent;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/lock-seats")
    @Idempotent
    @ResponseStatus(HttpStatus.CREATED)
    BookingResponse lockSeats(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody LockSeatsRequest request) {
        return bookingService.lockSeats(user, request);
    }

    @PostMapping
    @Idempotent
    @ResponseStatus(HttpStatus.CREATED)
    BookingResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody LockSeatsRequest request) {
        return bookingService.lockSeats(user, request);
    }

    @GetMapping("/{id}")
    BookingResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return bookingService.get(user, id);
    }

    @PostMapping("/{id}/cancel")
    BookingResponse cancel(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return bookingService.cancel(user, id);
    }
}
