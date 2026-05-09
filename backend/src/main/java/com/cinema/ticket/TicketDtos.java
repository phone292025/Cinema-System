package com.cinema.ticket;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cinema.booking.BookingDtos.BookingItemResponse;

import jakarta.validation.constraints.NotBlank;

public final class TicketDtos {
    private TicketDtos() {
    }

    public record TicketResponse(UUID id, UUID bookingId, String bookingCode, String ticketCode, TicketStatus status,
            Instant issuedAt, Instant usedAt, String movieTitle, String cinemaName, String hallName, Instant startTime,
            List<BookingItemResponse> seats, String qrUrl) {
        public static TicketResponse from(Ticket ticket) {
            return new TicketResponse(ticket.getId(), ticket.getBooking().getId(), ticket.getBooking().getBookingCode(),
                    ticket.getTicketCode(), ticket.getStatus(), ticket.getIssuedAt(), ticket.getUsedAt(),
                    ticket.getBooking().getShowtime().getMovie().getTitle(), ticket.getBooking().getShowtime().getHall().getCinema().getName(),
                    ticket.getBooking().getShowtime().getHall().getName(), ticket.getBooking().getShowtime().getStartTime(),
                    ticket.getBooking().getItems().stream().map(item -> new BookingItemResponse(item.getSeat().getId(),
                            item.getSeat().getRowLabel(), item.getSeat().getSeatNumber(), item.getSeat().getSeatType(), item.getPrice())).toList(),
                    "/tickets/" + ticket.getId() + "/qr");
        }
    }

    public record ValidateTicketRequest(String qrToken) {
    }

    public record ValidateTicketResponse(String ticketCode, String bookingCode, TicketStatus status, String movieTitle,
            String cinemaName, String hallName, Instant startTime, List<BookingItemResponse> seats, Instant validatedAt) {
        public static ValidateTicketResponse from(Ticket ticket) {
            return new ValidateTicketResponse(ticket.getTicketCode(), ticket.getBooking().getBookingCode(), ticket.getStatus(),
                    ticket.getBooking().getShowtime().getMovie().getTitle(), ticket.getBooking().getShowtime().getHall().getCinema().getName(),
                    ticket.getBooking().getShowtime().getHall().getName(), ticket.getBooking().getShowtime().getStartTime(),
                    ticket.getBooking().getItems().stream().map(item -> new BookingItemResponse(item.getSeat().getId(),
                            item.getSeat().getRowLabel(), item.getSeat().getSeatNumber(), item.getSeat().getSeatType(), item.getPrice())).toList(),
                    ticket.getUsedAt());
        }
    }
}
