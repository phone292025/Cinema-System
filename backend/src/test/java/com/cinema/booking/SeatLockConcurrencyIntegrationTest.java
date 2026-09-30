package com.cinema.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.cinema.auth.AuthUser;
import com.cinema.showtime.Showtime;
import com.cinema.showtime.ShowtimeRepository;
import com.cinema.showtime.ShowtimeSeat;
import com.cinema.showtime.ShowtimeSeatRepository;
import com.cinema.showtime.ShowtimeSeatStatus;
import com.cinema.user.User;
import com.cinema.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "app.jwt.secret=concurrency-test-jwt-secret-concurrency-test-jwt-secret",
        "app.ticket.secret=concurrency-test-ticket-secret-concurrency-test-ticket-secret",
        "app.seed.demo-users-enabled=true",
        "app.seed.catalog-enabled=true",
        "app.seed.demo-admin-password=concurrency-admin-password",
        "app.seed.demo-customer-password=concurrency-customer-password",
        "spring.task.scheduling.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class SeatLockConcurrencyIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cinema_test")
            .withUsername("cinema")
            .withPassword("cinema");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private UserRepository users;

    @Autowired
    private ShowtimeRepository showtimes;

    @Autowired
    private ShowtimeSeatRepository showtimeSeats;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookings;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @DynamicPropertySource
    static void containers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Test
    void onlyOneOfManyConcurrentRequestsWinsTheSameSeat() throws Exception {
        User customer = users.findByEmailIgnoreCase("customer@cinema.test").orElseThrow();
        Showtime showtime = latestShowtime();
        UUID seatId = availableSeatIds(showtime, 1).get(0);

        int attempts = 8;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        try {
            for (int i = 0; i < attempts; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        bookings.lockSeats(AuthUser.from(customer), new BookingDtos.LockSeatsRequest(showtime.getId(), List.of(seatId)));
                        succeeded.incrementAndGet();
                    } catch (Exception expectedForLosers) {
                    }
                });
            }
            start.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(seat(showtime, seatId).getStatus()).isEqualTo(ShowtimeSeatStatus.LOCKED);
    }

    @Test
    void expiringBookingDoesNotReleaseASeatAnotherBookingHasTakenOver() {
        User customer = users.findByEmailIgnoreCase("customer@cinema.test").orElseThrow();
        Showtime showtime = latestShowtime();
        UUID seatId = availableSeatIds(showtime, 1).get(0);

        BookingDtos.BookingResponse first = bookings.lockSeats(AuthUser.from(customer),
                new BookingDtos.LockSeatsRequest(showtime.getId(), List.of(seatId)));

        expire(first.id(), seatId, showtime.getId());

        BookingDtos.BookingResponse second = bookings.lockSeats(AuthUser.from(customer),
                new BookingDtos.LockSeatsRequest(showtime.getId(), List.of(seatId)));

        bookings.expireStaleBookings();

        ShowtimeSeat seat = seat(showtime, seatId);
        assertThat(seat.getStatus()).isEqualTo(ShowtimeSeatStatus.LOCKED);
        assertThat(seat.getLockedByBookingId()).isEqualTo(second.id());
        assertThat(redisTemplate.opsForValue().get(redisKey(showtime.getId(), seatId))).isEqualTo(second.id().toString());
        assertThat(bookingRepository.findById(first.id()).orElseThrow().getStatus()).isEqualTo(BookingStatus.EXPIRED);
    }

    private void expire(UUID bookingId, UUID seatId, UUID showtimeId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.setExpiresAt(Instant.now().minus(10, ChronoUnit.MINUTES));
        bookingRepository.save(booking);

        redisTemplate.delete(redisKey(showtimeId, seatId));

        ShowtimeSeat seat = seat(showtimeId, seatId);
        seat.setStatus(ShowtimeSeatStatus.AVAILABLE);
        seat.setLockedUntil(null);
        seat.setLockedByBookingId(null);
        showtimeSeats.save(seat);
    }

    private String redisKey(UUID showtimeId, UUID seatId) {
        return "lock:showtime:%s:seat:%s".formatted(showtimeId, seatId);
    }

    private Showtime latestShowtime() {
        return showtimes.findAll().stream()
                .max(Comparator.comparing(Showtime::getStartTime))
                .orElseThrow();
    }

    private List<UUID> availableSeatIds(Showtime showtime, int count) {
        return showtimeSeats.findByShowtimeIdOrderBySeatRowLabelAscSeatSeatNumberAsc(showtime.getId()).stream()
                .filter(seat -> seat.getStatus() == ShowtimeSeatStatus.AVAILABLE)
                .limit(count)
                .map(seat -> seat.getSeat().getId())
                .toList();
    }

    private ShowtimeSeat seat(Showtime showtime, UUID seatId) {
        return seat(showtime.getId(), seatId);
    }

    private ShowtimeSeat seat(UUID showtimeId, UUID seatId) {
        return showtimeSeats.findByShowtimeIdOrderBySeatRowLabelAscSeatSeatNumberAsc(showtimeId).stream()
                .filter(candidate -> candidate.getSeat().getId().equals(seatId))
                .findFirst()
                .orElseThrow();
    }
}
