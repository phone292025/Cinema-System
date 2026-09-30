package com.cinema.ticket;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

import com.cinema.audit.AuditLogService;
import com.cinema.auth.AuthUser;
import com.cinema.booking.Booking;
import com.cinema.booking.BookingAccess;
import com.cinema.booking.BookingEvent;
import com.cinema.booking.BookingRepository;
import com.cinema.booking.BookingStateMachine;
import com.cinema.booking.BookingStatus;
import com.cinema.common.ApiException;
import com.cinema.common.RecordedFailureException;
import com.cinema.common.SecretValidator;
import com.cinema.notification.NotificationService;
import com.cinema.ticket.TicketDtos.TicketResponse;
import com.cinema.ticket.TicketDtos.ValidateTicketResponse;
import com.cinema.user.User;
import com.cinema.user.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final Duration VALIDATION_GRACE = Duration.ofMinutes(30);

    private final TicketRepository tickets;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final BookingStateMachine stateMachine;
    private final NotificationService notifications;
    private final AuditLogService auditLogs;
    private final String ticketSecret;

    public TicketService(TicketRepository tickets, BookingRepository bookings, UserRepository users,
            BookingStateMachine stateMachine, NotificationService notifications, AuditLogService auditLogs,
            @Value("${app.ticket.secret}") String ticketSecret) {
        this.tickets = tickets;
        this.bookings = bookings;
        this.users = users;
        this.stateMachine = stateMachine;
        this.notifications = notifications;
        this.auditLogs = auditLogs;
        this.ticketSecret = SecretValidator.requireStrongSecret(ticketSecret, "app.ticket.secret");
    }

    @Transactional
    public void issue(UUID bookingId) {
        Booking booking = bookings.lockById(bookingId).orElse(null);
        if (booking == null) {
            log.warn("Skipping ticket issue: booking {} does not exist.", bookingId);
            return;
        }
        if (tickets.findByBookingId(bookingId).isPresent()) {
            return;
        }
        if (booking.getStatus() != BookingStatus.PAID && booking.getStatus() != BookingStatus.TICKET_ISSUED) {
            log.info("Skipping ticket issue: booking {} is {} and no longer needs a ticket.", bookingId, booking.getStatus());
            return;
        }
        createTicket(booking);
    }

    @Transactional(readOnly = true)
    public TicketResponse getForBooking(AuthUser user, UUID bookingId) {
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found."));
        BookingAccess.requireOwnerOrAdmin(user, booking);
        return tickets.findDetailedByBookingId(bookingId).map(TicketResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ticket is not ready yet."));
    }

    @Transactional(readOnly = true)
    public byte[] qrPng(AuthUser user, UUID ticketId) {
        Ticket ticket = tickets.findById(ticketId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ticket not found."));
        BookingAccess.requireOwnerOrAdmin(user, ticket.getBooking());
        return qr(rawToken(ticket.getBooking().getId(), ticket.getTicketCode()));
    }

    @Transactional(noRollbackFor = RecordedFailureException.class)
    public ValidateTicketResponse validateForStaff(AuthUser staff, String scannedCode, String qrToken) {
        return ValidateTicketResponse.from(validate(staff, scannedCode, qrToken));
    }

    @Transactional(noRollbackFor = RecordedFailureException.class)
    public Ticket validate(AuthUser staff, String scannedCode, String qrToken) {
        String rawToken = qrToken == null || qrToken.isBlank() ? scannedCode : qrToken;
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "QR token is required.");
        }
        Ticket ticket = tickets.findWithLockByQrTokenHash(hash(rawToken))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ticket not found."));
        if (ticket.getStatus() == TicketStatus.USED) {
            throw new ApiException(HttpStatus.CONFLICT, "Ticket was already validated.");
        }
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Booking was cancelled.");
        }
        if (ticket.getStatus() == TicketStatus.EXPIRED
                || ticket.getBooking().getShowtime().getStartTime().isBefore(Instant.now().minus(VALIDATION_GRACE))) {
            ticket.setStatus(TicketStatus.EXPIRED);
            throw new RecordedFailureException(HttpStatus.CONFLICT, "Showtime already ended.");
        }
        User validator = users.findById(staff.id()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Staff user not found."));
        Instant usedAt = Instant.now();
        int updated = tickets.markUsedIfIssued(ticket.getId(), usedAt, validator, TicketStatus.USED, TicketStatus.ISSUED);
        if (updated == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Ticket was validated by another device.");
        }
        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(usedAt);
        ticket.setValidatedBy(validator);
        auditLogs.record(staff, "TICKET_VALIDATED", "Ticket", ticket.getId().toString(), null, ticket.getTicketCode(), null);
        return ticket;
    }

    private void createTicket(Booking booking) {
        String ticketCode = "TCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        Ticket ticket = new Ticket();
        ticket.setBooking(booking);
        ticket.setTicketCode(ticketCode);
        ticket.setStatus(TicketStatus.ISSUED);
        ticket.setQrTokenHash(hash(rawToken(booking.getId(), ticketCode)));
        Ticket saved = tickets.save(ticket);
        if (booking.getStatus() == BookingStatus.PAID) {
            booking.setStatus(stateMachine.transition(BookingStatus.PAID, BookingEvent.TICKET_ISSUED));
            booking.setUpdatedAt(Instant.now());
        }
        notifications.create(booking.getUser().getId(), "BOOKING_CONFIRMED", "Your ticket is ready",
                "Show your QR ticket at the cinema entrance for " + booking.getShowtime().getMovie().getTitle() + ".",
                booking.getId());
        auditLogs.system("TICKET_ISSUED", "Ticket", saved.getId().toString(), saved.getTicketCode());
    }

    private String rawToken(UUID bookingId, String ticketCode) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(ticketSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((bookingId + ":" + ticketCode).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create QR token", ex);
        }
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash QR token", ex);
        }
    }

    private byte[] qr(String token) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 320, 320);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate QR code", ex);
        }
    }
}
