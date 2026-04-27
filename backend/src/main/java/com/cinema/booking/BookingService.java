package com.cinema.booking;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.cinema.auth.AuthUser;
import com.cinema.booking.BookingDtos.BookingResponse;
import com.cinema.booking.BookingDtos.LockSeatsRequest;
import com.cinema.common.ApiException;
import com.cinema.showtime.Showtime;
import com.cinema.showtime.ShowtimeRepository;
import com.cinema.showtime.ShowtimeSeat;
import com.cinema.showtime.ShowtimeSeatRepository;
import com.cinema.showtime.ShowtimeSeatStatus;
import com.cinema.user.User;
import com.cinema.user.UserRepository;
import com.cinema.user.UserRole;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    private final BookingRepository bookings;
    private final ShowtimeRepository showtimes;
    private final ShowtimeSeatRepository showtimeSeats;
    private final UserRepository users;
    private final SeatLockService seatLocks;
    private final Duration lockDuration;
    private final long cancelCutoffHours;

    public BookingService(BookingRepository bookings, ShowtimeRepository showtimes, ShowtimeSeatRepository showtimeSeats,
            UserRepository users, SeatLockService seatLocks,
            @Value("${app.booking.lock-minutes}") long lockMinutes,
            @Value("${app.booking.cancel-cutoff-hours}") long cancelCutoffHours) {
        this.bookings = bookings;
        this.showtimes = showtimes;
        this.showtimeSeats = showtimeSeats;
        this.users = users;
        this.seatLocks = seatLocks;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
        this.cancelCutoffHours = cancelCutoffHours;
    }

    @Transactional
    public BookingResponse lockSeats(AuthUser authUser, LockSeatsRequest request) {
        cleanupExpiredLocks();
        User user = users.findById(authUser.id()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found."));
        Showtime showtime = showtimes.findById(request.showtimeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Showtime not found."));

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
        booking.setTotalAmount(seats.stream().map(ShowtimeSeat::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add));
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
        });
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findUserBookings(UUID userId) {
        return bookings.findByUserIdOrderByCreatedAtDesc(userId).stream().map(BookingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse get(AuthUser authUser, UUID bookingId) {
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found."));
        assertOwnsOrAdmin(authUser, booking);
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse cancel(AuthUser authUser, UUID bookingId) {
        Booking booking = bookings.lockById(bookingId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found."));
        assertOwnsOrAdmin(authUser, booking);
        if (booking.getStatus() == BookingStatus.PAID
                && Instant.now().isAfter(booking.getShowtime().getStartTime().minus(Duration.ofHours(cancelCutoffHours)))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Booking can only be cancelled at least %d hours before showtime.".formatted(cancelCutoffHours));
        }
        if (booking.getStatus() == BookingStatus.PAID) {
            booking.setStatus(BookingStatus.REFUNDED);
        } else if (booking.getStatus() == BookingStatus.LOCKED || booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CANCELLED);
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Booking cannot be cancelled from status " + booking.getStatus());
        }
        releaseSeats(booking);
        booking.setUpdatedAt(Instant.now());
        return BookingResponse.from(booking);
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireStaleBookings() {
        Instant now = Instant.now();
        bookings.findByStatusInAndExpiresAtBefore(List.of(BookingStatus.LOCKED, BookingStatus.PENDING), now).forEach(booking -> {
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setUpdatedAt(now);
            releaseSeats(booking);
        });
        cleanupExpiredLocks();
    }

    @Transactional
    public void releaseSeats(Booking booking) {
        List<UUID> seatIds = booking.getItems().stream().map(item -> item.getSeat().getId()).toList();
        booking.getItems().forEach(item -> showtimeSeats.lockOne(booking.getShowtime().getId(), item.getSeat().getId()).ifPresent(showtimeSeat -> {
            if (showtimeSeat.getStatus() == ShowtimeSeatStatus.LOCKED || showtimeSeat.getStatus() == ShowtimeSeatStatus.BOOKED) {
                showtimeSeat.setStatus(ShowtimeSeatStatus.AVAILABLE);
                showtimeSeat.setLockedUntil(null);
            }
        }));
        seatLocks.release(booking.getShowtime().getId(), seatIds);
    }

    @Transactional
    public void markSeatsBooked(Booking booking) {
        booking.getItems().forEach(item -> showtimeSeats.lockOne(booking.getShowtime().getId(), item.getSeat().getId()).ifPresent(showtimeSeat -> {
            if (showtimeSeat.getStatus() == ShowtimeSeatStatus.BOOKED) {
                return;
            }
            if (showtimeSeat.getStatus() != ShowtimeSeatStatus.LOCKED) {
                throw new ApiException(HttpStatus.CONFLICT, "Seat is no longer locked for this booking.");
            }
            showtimeSeat.setStatus(ShowtimeSeatStatus.BOOKED);
            showtimeSeat.setLockedUntil(null);
        }));
        seatLocks.release(booking.getShowtime().getId(), booking.getItems().stream().map(item -> item.getSeat().getId()).toList());
    }

    private void cleanupExpiredLocks() {
        Instant now = Instant.now();
        showtimeSeats.findByStatusAndLockedUntilBefore(ShowtimeSeatStatus.LOCKED, now).forEach(seat -> {
            seat.setStatus(ShowtimeSeatStatus.AVAILABLE);
            seat.setLockedUntil(null);
        });
    }

    private void releaseIfDbLockExpired(ShowtimeSeat seat, Instant now) {
        if (seat.getStatus() == ShowtimeSeatStatus.LOCKED && seat.getLockedUntil() != null && seat.getLockedUntil().isBefore(now)) {
            seat.setStatus(ShowtimeSeatStatus.AVAILABLE);
            seat.setLockedUntil(null);
        }
    }

    private void assertOwnsOrAdmin(AuthUser authUser, Booking booking) {
        if (!booking.getUser().getId().equals(authUser.id()) && authUser.role() != UserRole.ADMIN && authUser.role() != UserRole.STAFF) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Booking does not belong to this user.");
        }
    }

    private String generateCode() {
        return "CBX-" + Instant.now().toString().substring(0, 10).replace("-", "") + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }
}
