package com.cinema.staff;

import java.util.List;

import com.cinema.auth.AuthUser;
import com.cinema.booking.BookingDtos.BookingResponse;
import com.cinema.showtime.ShowtimeDtos.ShowtimeResponse;
import com.cinema.ticket.TicketDtos.ValidateTicketRequest;
import com.cinema.ticket.TicketDtos.ValidateTicketResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/staff")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class StaffController {
    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping("/showtimes/today")
    List<ShowtimeResponse> todayShowtimes() {
        return staffService.todayShowtimes();
    }

    @GetMapping("/bookings/search")
    BookingResponse searchBooking(@RequestParam @NotBlank @Size(max = 40) String code) {
        return staffService.searchBooking(code);
    }

    @PostMapping("/tickets/{ticketCode}/validate")
    ValidateTicketResponse validate(@AuthenticationPrincipal AuthUser staff, @PathVariable String ticketCode,
            @Valid @RequestBody(required = false) ValidateTicketRequest request) {
        return staffService.validate(staff, ticketCode, request == null ? null : request.qrToken());
    }
}
