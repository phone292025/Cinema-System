package com.cinema.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
    @Test
    void validatesTicketByVisibleTicketCodeWhenQrTokenIsNotProvided() {
        TicketRepository tickets = mock(TicketRepository.class);
        UserRepository users = mock(UserRepository.class);
        AuditLogService auditLogs = mock(AuditLogService.class);
        Ticket ticket = issuedTicket("TCK-12345678");
        User staffUser = user(UserRole.STAFF);
        AuthUser staff = AuthUser.from(staffUser);

        when(tickets.findWithLockByQrTokenHash(any())).thenReturn(Optional.empty());
        when(tickets.findWithLockByTicketCode("TCK-12345678")).thenReturn(Optional.of(ticket));
        when(users.findById(staff.id())).thenReturn(Optional.of(staffUser));
        when(tickets.markUsedIfIssued(eq(ticket.getId()), any(), eq(staffUser), eq(TicketStatus.USED), eq(TicketStatus.ISSUED)))
                .thenReturn(1);

        TicketService service = new TicketService(tickets, mock(BookingRepository.class), users, new BookingStateMachine(),
                mock(NotificationService.class), auditLogs, "test-ticket-secret");

        Ticket validated = service.validate(staff, "TCK-12345678", null);

        assertThat(validated.getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(validated.getValidatedBy()).isEqualTo(staffUser);
        verify(tickets).findWithLockByTicketCode("TCK-12345678");
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
}
