package com.cinema.staff;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.cinema.auth.AuthUser;
import com.cinema.booking.BookingDtos.BookingResponse;
import com.cinema.booking.BookingRepository;
import com.cinema.common.ApiException;
import com.cinema.showtime.ShowtimeDtos.ShowtimeResponse;
import com.cinema.showtime.ShowtimeRepository;
import com.cinema.ticket.TicketDtos.ValidateTicketResponse;
import com.cinema.ticket.TicketService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffService {
    private final ShowtimeRepository showtimes;
    private final BookingRepository bookings;
    private final TicketService tickets;
    private final ZoneId zone;

    public StaffService(ShowtimeRepository showtimes, BookingRepository bookings, TicketService tickets, ZoneId businessZone) {
        this.showtimes = showtimes;
        this.bookings = bookings;
        this.tickets = tickets;
        this.zone = businessZone;
    }

    @Transactional(readOnly = true)
    public List<ShowtimeResponse> todayShowtimes() {
        LocalDate today = LocalDate.now(zone);
        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();
        return showtimes.findByStartTimeBetweenOrderByStartTimeAsc(start, end).stream().map(ShowtimeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse searchBooking(String code) {
        return bookings.findByBookingCodeIgnoreCase(code.trim())
                .map(BookingResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking code not found."));
    }

    public ValidateTicketResponse validate(AuthUser staff, String ticketCode, String qrToken) {
        return tickets.validateForStaff(staff, ticketCode, qrToken);
    }
}
