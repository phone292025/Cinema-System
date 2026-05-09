package com.cinema.staff;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import com.cinema.auth.AuthUser;
import com.cinema.booking.BookingDtos.BookingResponse;
import com.cinema.booking.BookingRepository;
import com.cinema.common.ApiException;
import com.cinema.showtime.ShowtimeDtos.ShowtimeResponse;
import com.cinema.showtime.ShowtimeRepository;
import com.cinema.ticket.Ticket;
import com.cinema.ticket.TicketDtos.ValidateTicketRequest;
import com.cinema.ticket.TicketDtos.ValidateTicketResponse;
import com.cinema.ticket.TicketService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
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
@Transactional(readOnly = true)
public class StaffController {
    private final ShowtimeRepository showtimes;
    private final BookingRepository bookings;
    private final TicketService ticketService;

    public StaffController(ShowtimeRepository showtimes, BookingRepository bookings, TicketService ticketService) {
        this.showtimes = showtimes;
        this.bookings = bookings;
        this.ticketService = ticketService;
    }

    @GetMapping("/showtimes/today")
    List<ShowtimeResponse> todayShowtimes() {
        var start = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        var end = start.plusSeconds(24 * 60 * 60);
        return showtimes.findByStartTimeBetweenOrderByStartTimeAsc(start, end).stream().map(ShowtimeResponse::from).toList();
    }

    @GetMapping("/bookings/search")
    BookingResponse searchBooking(@RequestParam String code) {
        return bookings.findByBookingCodeIgnoreCase(code)
                .map(BookingResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking code not found."));
    }

    @PostMapping("/tickets/{ticketCode}/validate")
    @Transactional
    ValidateTicketResponse validate(@AuthenticationPrincipal AuthUser staff, @PathVariable String ticketCode,
            @Valid @RequestBody(required = false) ValidateTicketRequest request) {
        Ticket ticket = ticketService.validate(staff, ticketCode, request == null ? null : request.qrToken());
        return ValidateTicketResponse.from(ticket);
    }
}
