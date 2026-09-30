package com.cinema.booking;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.cinema.audit.AuditLogService;
import com.cinema.auth.AuthUser;
import com.cinema.booking.BookingDtos.BookingResponse;
import com.cinema.booking.BookingDtos.LockSeatsRequest;
import com.cinema.common.ApiException;
import com.cinema.common.SchedulerGuard;
import com.cinema.notification.NotificationService;
import com.cinema.payment.PaymentRepository;
import com.cinema.payment.PaymentStatus;
import com.cinema.showtime.SeatEvent;
import com.cinema.showtime.SeatEventPublisher;
import com.cinema.showtime.SeatEventType;
import com.cinema.showtime.Showtime;
import com.cinema.showtime.ShowtimeRepository;
import com.cinema.showtime.ShowtimeSeat;
import com.cinema.showtime.ShowtimeSeatRepository;
import com.cinema.showtime.ShowtimeSeatStatus;
import com.cinema.ticket.TicketRepository;
import com.cinema.ticket.TicketStatus;
import com.cinema.user.User;
import com.cinema.user.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_SUFFIX_LENGTH = 8;
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final BookingRepository bookings;
    private final ShowtimeRepository showtimes;
    private final ShowtimeSeatRepository showtimeSeats;
    private final UserRepository users;
    private final PaymentRepository payments;
    private final SeatLockService seatLocks;
    private final BookingPriceCalculator priceCalculator;
    private final BookingStateMachine stateMachine;
    private final SeatEventPublisher seatEvents;
    private final AuditLogService auditLogs;
    private final TicketRepository tickets;
    private final SchedulerGuard schedulerGuard;
    private final NotificationService notifications;
    private final Duration lockDuration;
    private final long cancelCutoffHours;
    private final SecureRandom random = new SecureRandom();

    public BookingService(BookingRepository bookings, ShowtimeRepository showtimes, ShowtimeSeatRepository showtimeSeats,
            UserRepository users, PaymentRepository payments, SeatLockService seatLocks, BookingPriceCalculator priceCalculator,
            BookingStateMachine stateMachine, SeatEventPublisher seatEvents, AuditLogService auditLogs, TicketRepository tickets,
            SchedulerGuard schedulerGuard, NotificationService notifications,
            @Value("${app.booking.lock-minutes}") long lockMinutes,
            @Value("${app.booking.cancel-cutoff-hours}") long cancelCutoffHours) {
        this.bookings = bookings;
        this.showtimes = showtimes;
        this.showtimeSeats = showtimeSeats;
        this.users = users;
        this.payments = payments;
        this.seatLocks = seatLocks;
        this.priceCalculator = priceCalculator;
        this.stateMachine = stateMachine;
        this.seatEvents = seatEvents;
        this.auditLogs = auditLogs;
        this.tickets = tickets;
        this.schedulerGuard = schedulerGuard;
        this.notifications = notifications;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
        this.cancelCutoffHours = cancelCutoffHours;
    }

    @Transactional
    public BookingResponse lockSeats(AuthUser authUser, LockSeatsRequest request) {
        User user = users.findById(authUser.id()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found."));
        Showtime showtime = showtimes.findById(request.showtimeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Showtime not found."));
        if (!showtime.getStartTime().isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This showtime has already started.");
        }

        List<UUID> uniqueSeatIds = request.seatIds().stream().distinct().toList();
        List<ShowtimeSeat> seats = showtimeSeats.lockByShowtimeAndSeatIds(showtime.getId(), uniqueSeatIds);
        if (seats.size() != uniqueSeatIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One or more seats do not belong to this showtime.");
        }

        Instant now = Instant.now();
        for (ShowtimeSeat seat : seats) {
            releaseIfDbLockExpired(seat, now);
            if (seat.getStatus() != ShowtimeSeatStatus.AVAILABLE || seatLocks.isLocked(showtime.getId(), seat.getSeat().getId())) {
                throw new ApiException(HttpStatus.CONFLICT, "One or more selected seats are no longer available.");
            }
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setBookingCode(generateCode());
        booking.setStatus(BookingStatus.LOCKED);
        booking.setExpiresAt(now.plus(lockDuration));
        booking.setTotalAmount(priceCalculator.total(seats));
        seats.stream()
                .sorted(Comparator.comparing((ShowtimeSeat seat) -> seat.getSeat().getRowLabel()).thenComparing(seat -> seat.getSeat().getSeatNumber()))
                .forEach(seat -> {
                    BookingItem item = new BookingItem();
                    item.setSeat(seat.getSeat());
                    item.setPrice(seat.getPrice());
                    booking.addItem(item);
                });
        bookings.saveAndFlush(booking);

        List<String> keys = seatLocks.lock(showtime.getId(), uniqueSeatIds, booking.getId(), lockDuration);
        if (keys.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "One or more selected seats are already locked.");
        }

        seats.forEach(seat -> {
            seat.setStatus(ShowtimeSeatStatus.LOCKED);
            seat.setLockedUntil(booking.getExpiresAt());
            seat.setLockedByBookingId(booking.getId());
            seatEvents.publish(SeatEvent.from(SeatEventType.SEAT_LOCKED, seat));
        });
        auditLogs.record(authUser, "SEAT_LOCKED", "Booking", booking.getId().toString(), null,
                uniqueSeatIds.stream().map(UUID::toString).toList().toString(), null);
        return describe(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findUserBookings(UUID userId) {
        return bookings.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::describe).toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse get(AuthUser authUser, UUID bookingId) {
        Booking booking = bookings.findDetailedById(bookingId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found."));
        BookingAccess.requireOwnerOrAdmin(authUser, booking);
        return describe(booking);
    }

    @Transactional
    public BookingResponse cancel(AuthUser authUser, UUID bookingId) {
        Booking booking = bookings.lockById(bookingId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found."));
        BookingAccess.requireOwnerOrAdmin(authUser, booking);
        boolean paid = booking.getStatus() == BookingStatus.PAID || booking.getStatus() == BookingStatus.TICKET_ISSUED;
        if (paid && Instant.now().isAfter(booking.getShowtime().getStartTime().minus(Duration.ofHours(cancelCutoffHours)))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Booking can only be cancelled at least %d hours before showtime.".formatted(cancelCutoffHours));
        }
        if (paid) {
            booking.setStatus(stateMachine.transition(booking.getStatus(), BookingEvent.REFUND_REQUESTED));
            booking.setStatus(stateMachine.transition(booking.getStatus(), BookingEvent.REFUNDED));
            payments.findFirstByBookingIdAndStatus(booking.getId(), PaymentStatus.SUCCEEDED)
                    .ifPresent(payment -> payment.setStatus(PaymentStatus.REFUNDED));
            notifications.create(booking.getUser().getId(), "BOOKING_REFUNDED", "Booking cancelled and refunded",
                    "Booking %s for %s was cancelled and $%s has been refunded.".formatted(booking.getBookingCode(),
                            booking.getShowtime().getMovie().getTitle(), booking.getTotalAmount().setScale(2)),
                    booking.getId());
        } else if (booking.getStatus() == BookingStatus.LOCKED || booking.getStatus() == BookingStatus.PAYMENT_PENDING) {
            booking.setStatus(stateMachine.transition(booking.getStatus(), BookingEvent.CANCELLED));
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Booking cannot be cancelled from status " + booking.getStatus());
        }
        tickets.findByBookingId(booking.getId()).ifPresent(ticket -> ticket.setStatus(TicketStatus.CANCELLED));
        releaseSeats(booking);
        booking.setUpdatedAt(Instant.now());
        auditLogs.record(authUser, "BOOKING_CANCELLED", "Booking", booking.getId().toString(), null, booking.getStatus().name(), null);
        return describe(booking);
    }

    @Transactional
    public void expire(Booking booking) {
        if (stateMachine.canTransition(booking.getStatus(), BookingEvent.EXPIRED)) {
            booking.setStatus(stateMachine.transition(booking.getStatus(), BookingEvent.EXPIRED));
            booking.setUpdatedAt(Instant.now());
            auditLogs.system("BOOKING_EXPIRED", "Booking", booking.getId().toString(), booking.getStatus().name());
        }
        releaseSeats(booking);
    }

    @Scheduled(fixedDelay = 60_000)
    public void expireStaleBookingsJob() {
        schedulerGuard.runExclusively("booking-expiry", this::expireStaleBookings);
    }

    @Transactional
    public void expireStaleBookings() {
        bookings.findByStatusInAndExpiresAtBefore(List.of(BookingStatus.LOCKED, BookingStatus.PAYMENT_PENDING), Instant.now())
                .forEach(this::expire);
        cleanupExpiredLocks();
    }

    @Scheduled(fixedDelay = 60_000)
    public void releaseExpiredSeatLocksJob() {
        schedulerGuard.runExclusively("seat-lock-expiry", this::releaseExpiredSeatLocks);
    }

    @Transactional
    public void releaseExpiredSeatLocks() {
        cleanupExpiredLocks();
    }

    @Transactional
    public void releaseSeats(Booking booking) {
        List<UUID> seatIds = booking.getItems().stream().map(item -> item.getSeat().getId()).toList();
        booking.getItems().forEach(item -> showtimeSeats.lockOne(booking.getShowtime().getId(), item.getSeat().getId()).ifPresent(showtimeSeat -> {
            if (!holds(showtimeSeat, booking)) {
                return;
            }
            if (showtimeSeat.getStatus() == ShowtimeSeatStatus.LOCKED || showtimeSeat.getStatus() == ShowtimeSeatStatus.BOOKED) {
                markAvailable(showtimeSeat, SeatEventType.SEAT_RELEASED);
            }
        }));
        seatLocks.release(booking.getShowtime().getId(), seatIds, booking.getId());
    }

    @Transactional
    public void markSeatsBooked(Booking booking) {
        booking.getItems().forEach(item -> showtimeSeats.lockOne(booking.getShowtime().getId(), item.getSeat().getId()).ifPresent(showtimeSeat -> {
            if (!holds(showtimeSeat, booking)) {
                throw new ApiException(HttpStatus.CONFLICT, "Seat is now held by another booking.");
            }
            if (showtimeSeat.getStatus() == ShowtimeSeatStatus.BOOKED) {
                return;
            }
            if (showtimeSeat.getStatus() != ShowtimeSeatStatus.LOCKED) {
                throw new ApiException(HttpStatus.CONFLICT, "Seat is no longer locked for this booking.");
            }
            showtimeSeat.setStatus(ShowtimeSeatStatus.BOOKED);
            showtimeSeat.setLockedUntil(null);
            showtimeSeat.setLockedByBookingId(booking.getId());
            seatEvents.publish(SeatEvent.from(SeatEventType.SEAT_BOOKED, showtimeSeat));
        }));
        seatLocks.release(booking.getShowtime().getId(),
                booking.getItems().stream().map(item -> item.getSeat().getId()).toList(), booking.getId());
    }

    private BookingResponse describe(Booking booking) {
        return BookingResponse.from(booking, Duration.ofHours(cancelCutoffHours));
    }

    private boolean holds(ShowtimeSeat seat, Booking booking) {
        return seat.getLockedByBookingId() == null || seat.getLockedByBookingId().equals(booking.getId());
    }

    private void markAvailable(ShowtimeSeat seat, SeatEventType eventType) {
        seat.setStatus(ShowtimeSeatStatus.AVAILABLE);
        seat.setLockedUntil(null);
        seat.setLockedByBookingId(null);
        seatEvents.publish(SeatEvent.from(eventType, seat));
    }

    private void cleanupExpiredLocks() {
        showtimeSeats.findByStatusAndLockedUntilBefore(ShowtimeSeatStatus.LOCKED, Instant.now())
                .forEach(seat -> markAvailable(seat, SeatEventType.SEAT_EXPIRED));
    }

    private void releaseIfDbLockExpired(ShowtimeSeat seat, Instant now) {
        if (seat.getStatus() == ShowtimeSeatStatus.LOCKED && seat.getLockedUntil() != null && seat.getLockedUntil().isBefore(now)) {
            markAvailable(seat, SeatEventType.SEAT_EXPIRED);
        }
    }

    private String generateCode() {
        String date = LocalDate.now(ZoneOffset.UTC).format(CODE_DATE);
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = "CBX-" + date + "-" + randomSuffix();
            if (!bookings.existsByBookingCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique booking code.");
    }

    private String randomSuffix() {
        StringBuilder suffix = new StringBuilder(CODE_SUFFIX_LENGTH);
        for (int i = 0; i < CODE_SUFFIX_LENGTH; i++) {
            suffix.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return suffix.toString();
    }
}
