package com.cinema.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.cinema.auth.AuthUser;
import com.cinema.common.RecordedFailureException;
import com.cinema.notification.NotificationRepository;
import com.cinema.outbox.OutboxEvent;
import com.cinema.outbox.OutboxEventRepository;
import com.cinema.outbox.OutboxStatus;
import com.cinema.outbox.OutboxWorker;
import com.cinema.payment.PaymentDtos;
import com.cinema.payment.PaymentRepository;
import com.cinema.payment.PaymentService;
import com.cinema.payment.PaymentStatus;
import com.cinema.reporting.ReportingService;
import com.cinema.showtime.Showtime;
import com.cinema.showtime.ShowtimeRepository;
import com.cinema.showtime.ShowtimeSeat;
import com.cinema.showtime.ShowtimeSeatRepository;
import com.cinema.showtime.ShowtimeSeatStatus;
import com.cinema.ticket.Ticket;
import com.cinema.ticket.TicketRepository;
import com.cinema.ticket.TicketService;
import com.cinema.ticket.TicketStatus;
import com.cinema.user.User;
import com.cinema.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "app.jwt.secret=integration-test-jwt-secret-integration-test-jwt-secret",
        "app.ticket.secret=integration-test-ticket-secret-integration-test-ticket-secret",
        "app.seed.demo-users-enabled=true",
        "app.seed.catalog-enabled=true",
        "app.seed.demo-admin-password=integration-admin-password",
        "app.seed.demo-customer-password=integration-customer-password",
        "spring.task.scheduling.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class BookingFlowIntegrationTest {
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
    private BookingService bookings;

    @Autowired
    private PaymentService payments;

    @Autowired
    private OutboxWorker outboxWorker;

    @Autowired
    private TicketRepository tickets;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxEventRepository outboxEvents;

    @Autowired
    private ReportingService reporting;

    @Autowired
    private NotificationRepository notificationRepository;

    @DynamicPropertySource
    static void containers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Test
    void booksPaidTicketAndValidatesItWithRealPostgresAndRedis() {
        User customer = users.findByEmailIgnoreCase("customer@cinema.test").orElseThrow();
        User admin = users.findByEmailIgnoreCase("admin@cinema.test").orElseThrow();
        Showtime showtime = showtimes.findAll().stream()
                .min(Comparator.comparing(Showtime::getStartTime))
                .orElseThrow();
        List<UUID> seatIds = showtimeSeats.findByShowtimeIdOrderBySeatRowLabelAscSeatSeatNumberAsc(showtime.getId()).stream()
                .filter(seat -> seat.getStatus() == ShowtimeSeatStatus.AVAILABLE)
                .limit(2)
                .map(seat -> seat.getSeat().getId())
                .toList();

        BookingDtos.BookingResponse locked = bookings.lockSeats(AuthUser.from(customer), new BookingDtos.LockSeatsRequest(showtime.getId(), seatIds));
        PaymentDtos.PaymentResponse initiated = payments.initiate(AuthUser.from(customer), new PaymentDtos.InitiatePaymentRequest(locked.id(), "MOCK"));
        PaymentDtos.PaymentResponse paid = payments.mockCallback(AuthUser.from(customer),
                new PaymentDtos.MockCallbackRequest(initiated.paymentReference(), PaymentStatus.SUCCEEDED));
        outboxWorker.processOutboxEvents();
        Ticket issued = tickets.findByBookingId(locked.id()).orElseThrow();
        Ticket validated = ticketService.validate(AuthUser.from(admin), issued.getTicketCode(), rawTicketToken(issued));
        BookingDtos.BookingResponse completed = bookings.get(AuthUser.from(customer), locked.id());

        assertThat(locked.status()).isEqualTo(BookingStatus.LOCKED);
        assertThat(paid.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(completed.status()).isEqualTo(BookingStatus.TICKET_ISSUED);
        assertThat(validated.getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(showtimeSeats.findByShowtimeIdOrderBySeatRowLabelAscSeatSeatNumberAsc(showtime.getId()).stream()
                .filter(seat -> seatIds.contains(seat.getSeat().getId()))
                .map(ShowtimeSeat::getStatus))
                .containsOnly(ShowtimeSeatStatus.BOOKED);
    }

    @Test
    void refundBeforeTicketIssueDoesNotBlockTheOutboxAndLeavesRevenue() {
        User customer = users.findByEmailIgnoreCase("customer@cinema.test").orElseThrow();
        Showtime showtime = showtimeStartingAfter(Duration.ofHours(3));
        List<UUID> seatIds = availableSeatIds(showtime, 2);

        BookingDtos.BookingResponse refunded = payFor(customer, showtime, List.of(seatIds.get(0)));
        BigDecimal revenueWhilePaid = reporting.summary().revenue();
        bookings.cancel(AuthUser.from(customer), refunded.id());
        BigDecimal revenueAfterRefund = reporting.summary().revenue();
        BookingDtos.BookingResponse kept = payFor(customer, showtime, List.of(seatIds.get(1)));

        outboxWorker.processOutboxEvents();

        assertThat(tickets.findByBookingId(kept.id())).isPresent();
        assertThat(tickets.findByBookingId(refunded.id())).isEmpty();
        assertThat(bookings.get(AuthUser.from(customer), refunded.id()).status()).isEqualTo(BookingStatus.REFUNDED);
        assertThat(paymentRepository.findFirstByBookingIdAndStatus(refunded.id(), PaymentStatus.REFUNDED)).isPresent();
        assertThat(revenueWhilePaid.subtract(revenueAfterRefund)).isEqualByComparingTo(refunded.totalAmount());
        assertThat(notificationRepository.findTop100ByUserIdOrderByCreatedAtDesc(customer.getId()))
                .anySatisfy(notification -> {
                    assertThat(notification.getType()).isEqualTo("BOOKING_REFUNDED");
                    assertThat(notification.getBookingId()).isEqualTo(refunded.id());
                });
        assertThat(outboxEvents.findAll()).filteredOn(event -> refunded.id().equals(event.getAggregateId()))
                .extracting(OutboxEvent::getStatus)
                .containsOnly(OutboxStatus.PROCESSED);
    }

    @Test
    void expiredHoldIsRecordedWhenPaymentIsAttempted() {
        User customer = users.findByEmailIgnoreCase("customer@cinema.test").orElseThrow();
        Showtime showtime = showtimeStartingAfter(Duration.ofHours(1));
        UUID seatId = availableSeatIds(showtime, 1).get(0);
        BookingDtos.BookingResponse locked = bookings.lockSeats(AuthUser.from(customer),
                new BookingDtos.LockSeatsRequest(showtime.getId(), List.of(seatId)));
        Booking booking = bookingRepository.findById(locked.id()).orElseThrow();
        booking.setExpiresAt(Instant.now().minusSeconds(60));
        bookingRepository.save(booking);

        assertThatThrownBy(() -> payments.initiate(AuthUser.from(customer), new PaymentDtos.InitiatePaymentRequest(locked.id(), "MOCK")))
                .isInstanceOf(RecordedFailureException.class);

        assertThat(bookingRepository.findById(locked.id()).orElseThrow().getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(showtimeSeats.findByShowtimeIdOrderBySeatRowLabelAscSeatSeatNumberAsc(showtime.getId()).stream()
                .filter(seat -> seat.getSeat().getId().equals(seatId))
                .map(ShowtimeSeat::getStatus))
                .containsExactly(ShowtimeSeatStatus.AVAILABLE);
    }

    private BookingDtos.BookingResponse payFor(User customer, Showtime showtime, List<UUID> seatIds) {
        BookingDtos.BookingResponse locked = bookings.lockSeats(AuthUser.from(customer), new BookingDtos.LockSeatsRequest(showtime.getId(), seatIds));
        PaymentDtos.PaymentResponse initiated = payments.initiate(AuthUser.from(customer), new PaymentDtos.InitiatePaymentRequest(locked.id(), "MOCK"));
        payments.mockCallback(AuthUser.from(customer), new PaymentDtos.MockCallbackRequest(initiated.paymentReference(), PaymentStatus.SUCCEEDED));
        return locked;
    }

    private Showtime showtimeStartingAfter(Duration lead) {
        Instant earliest = Instant.now().plus(lead);
        return showtimes.findAll().stream()
                .filter(showtime -> showtime.getStartTime().isAfter(earliest))
                .min(Comparator.comparing(Showtime::getStartTime))
                .orElseThrow();
    }

    private List<UUID> availableSeatIds(Showtime showtime, int count) {
        return showtimeSeats.findByShowtimeIdOrderBySeatRowLabelAscSeatSeatNumberAsc(showtime.getId()).stream()
                .filter(seat -> seat.getStatus() == ShowtimeSeatStatus.AVAILABLE)
                .limit(count)
                .map(seat -> seat.getSeat().getId())
                .toList();
    }

    private String rawTicketToken(Ticket ticket) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec("integration-test-ticket-secret-integration-test-ticket-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((ticket.getBooking().getId() + ":" + ticket.getTicketCode()).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create test QR token", ex);
        }
    }
}
