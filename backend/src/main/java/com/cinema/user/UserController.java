package com.cinema.user;

import java.util.List;

import com.cinema.auth.AuthUser;
import com.cinema.booking.BookingDtos.BookingResponse;
import com.cinema.booking.BookingService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me")
public class UserController {
    private final BookingService bookingService;

    public UserController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/profile")
    UserProfileResponse profile(@AuthenticationPrincipal AuthUser user) {
        return new UserProfileResponse(user.id(), user.name(), user.email(), user.role().name());
    }

    @GetMapping("/bookings")
    List<BookingResponse> bookings(@AuthenticationPrincipal AuthUser user) {
        return bookingService.findUserBookings(user.id());
    }
}
