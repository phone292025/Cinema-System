package com.cinema.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import com.cinema.audit.AuditLogService;
import com.cinema.auth.AuthUser;
import com.cinema.booking.Booking;
import com.cinema.booking.BookingRepository;
import com.cinema.booking.BookingStateMachine;
import com.cinema.notification.NotificationService;
import com.cinema.showtime.Showtime;
import com.cinema.user.User;
import com.cinema.user.UserRepository;
import com.cinema.user.UserRole;

import org.junit.jupiter.api.Test;

class TicketServiceTest {
    private static final String TICKET_SECRET = "unit-test-ticket-secret-unit-test-ticket-secret";

    @Test
    void validatesTicketByQrTokenHash() {
        TicketRepository tickets = mock(TicketRepository.class);
        UserRepository users = mock(UserRepository.class);
        AuditLogService auditLogs = mock(AuditLogService.class);
        Ticket ticket = issuedTicket("TCK-12345678");
        User staffUser = user(UserRole.STAFF);
        AuthUser staff = AuthUser.from(staffUser);
        String qrToken = "signed-qr-token";

        when(tickets.findWithLockByQrTokenHash(hash(qrToken))).thenReturn(Optional.of(ticket));
        when(users.findById(staff.id())).thenReturn(Optional.of(staffUser));
        when(tickets.markUsedIfIssued(eq(ticket.getId()), any(), eq(staffUser), eq(TicketStatus.USED), eq(TicketStatus.ISSUED)))
                .thenReturn(1);

        TicketService service = new TicketService(tickets, mock(BookingRepository.class), users, new BookingStateMachine(),
                mock(NotificationService.class), auditLogs, TICKET_SECRET);

        Ticket validated = service.validate(staff, "TCK-12345678", qrToken);

        assertThat(validated.getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(validated.getValidatedBy()).isEqualTo(staffUser);
        verify(tickets, never()).findWithLockByTicketCode(anyString());
    }

    @Test
    void rejectsVisibleTicketCodeWithoutQrToken() {
        TicketRepository tickets = mock(TicketRepository.class);
        UserRepository users = mock(UserRepository.class);
        TicketService service = new TicketService(tickets, mock(BookingRepository.class), users, new BookingStateMachine(),
                mock(NotificationService.class), mock(AuditLogService.class), TICKET_SECRET);

        when(tickets.findWithLockByQrTokenHash(hash("TCK-12345678"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validate(userAuth(UserRole.STAFF), "TCK-12345678", null))
                .hasMessage("Ticket not found.");
        verify(tickets, never()).findWithLockByTicketCode(anyString());
    }

    private Ticket issuedTicket(String ticketCode) {
        Showtime showtime = new Showtime();
        showtime.setStartTime(Instant.now().plusSeconds(3600));

        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setShowtime(showtime);

        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        ticket.setBooking(booking);
        ticket.setTicketCode(ticketCode);
        ticket.setStatus(TicketStatus.ISSUED);
        return ticket;
    }

    private User user(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Staff");
        user.setEmail("staff@example.com");
        user.setRole(role);
        return user;
    }

    private AuthUser userAuth(UserRole role) {
        return AuthUser.from(user(role));
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash token", ex);
        }
    }
}
