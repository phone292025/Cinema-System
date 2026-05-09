package com.cinema.ticket;

import java.util.UUID;

import com.cinema.auth.AuthUser;
import com.cinema.ticket.TicketDtos.TicketResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@Transactional(readOnly = true)
public class TicketController {
    private final TicketService tickets;

    public TicketController(TicketService tickets) {
        this.tickets = tickets;
    }

    @GetMapping("/bookings/{id}/ticket")
    TicketResponse ticket(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return TicketResponse.from(tickets.getForBooking(user, id));
    }

    @GetMapping(value = "/tickets/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    byte[] qr(@AuthenticationPrincipal AuthUser user, @PathVariable UUID id) {
        return tickets.qrPng(user, id);
    }
}
