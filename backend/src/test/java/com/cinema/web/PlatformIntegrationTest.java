package com.cinema.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.cinema.auth.AuthDtos.AuthResponse;
import com.cinema.auth.AuthDtos.RefreshRequest;
import com.cinema.auth.AuthService;
import com.cinema.auth.AuthUser;
import com.cinema.auth.JwtService;
import com.cinema.common.ApiException;
import com.cinema.idempotency.IdempotencyService;
import com.cinema.idempotency.Idempotent;
import com.cinema.notification.NotificationService;
import com.cinema.user.User;
import com.cinema.user.UserRepository;
import com.cinema.user.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "app.jwt.secret=platform-it-jwt-secret-Hk3Vq9Lz2Xm7Np4R",
        "app.ticket.secret=platform-it-ticket-secret-Bw6Tj1Ys8Fc5Gd0K",
        "app.rate-limit.enabled=false",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
@Import(PlatformIntegrationTest.IdempotencyProbeController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class PlatformIntegrationTest {
    private static final String PASSWORD = "platform-test-password";

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cinema_test")
            .withUsername("cinema")
            .withPassword("cinema");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthService authService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private IdempotencyProbeController probe;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void containers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Test
    void healthProbesArePublicAndRevealNoDetails() throws Exception {
        for (String path : List.of("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness")) {
            mvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components").doesNotExist());
        }
        mvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/env").header("Authorization", bearer(createUser(UserRole.ADMIN))))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrationNormalizesEmailAndRejectsDuplicatesWithConflict() throws Exception {
        String email = "Flow." + UUID.randomUUID() + "@Example.COM";

        JsonNode registered = json(mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Flow User", "email", email, "password", PASSWORD))))
                .andExpect(status().isCreated()));
        assertThat(registered.at("/user/email").asText()).isEqualTo(email.toLowerCase());

        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Flow User", "email", email.toUpperCase(), "password", PASSWORD))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered."));

        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No Password\",\"email\":\"nopass@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("password")));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email.toUpperCase(), "password", PASSWORD))))
                .andExpect(status().isOk());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "nobody-" + UUID.randomUUID() + "@example.com", "password", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void reusingARotatedRefreshTokenRevokesTheWholeSession() throws Exception {
        User user = createUser(UserRole.CUSTOMER);
        String first = login(user).path("refreshToken").asText();

        JsonNode rotated = json(mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("refreshToken", first))))
                .andExpect(status().isOk()));
        String second = rotated.path("refreshToken").asText();
        mvc.perform(get("/users/me/profile").header("Authorization", "Bearer " + rotated.path("accessToken").asText()))
                .andExpect(status().isOk());

        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("refreshToken", first))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("refreshToken", second))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentRefreshesWithTheSameTokenSucceedOnlyOnce() throws Exception {
        User user = createUser(UserRole.CUSTOMER);
        String refreshToken = login(user).path("refreshToken").asText();

        List<Object> outcomes = race(2, () -> authService.refresh(new RefreshRequest(refreshToken)));

        assertThat(outcomes).filteredOn(AuthResponse.class::isInstance).hasSize(1);
        assertThat(outcomes).filteredOn(ApiException.class::isInstance)
                .singleElement()
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void idempotentEndpointReplaysTheStoredResponseAndStatus() throws Exception {
        String token = bearer(createUser(UserRole.CUSTOMER));
        String key = "replay-" + UUID.randomUUID();
        int callsBefore = probe.callCount();

        String first = mvc.perform(idempotentPost(token, key, "{\"value\":\"a\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String replay = mvc.perform(idempotentPost(token, key, "{\"value\":\"a\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(replay)).isEqualTo(objectMapper.readTree(first));
        assertThat(probe.callCount()).isEqualTo(callsBefore + 1);
        assertThat(jdbc.queryForObject("select status_code from idempotency_keys where key_value = ?", Integer.class, key))
                .isEqualTo(201);
    }

    @Test
    void idempotencyKeyReusedWithDifferentBodyIsRejected() throws Exception {
        String token = bearer(createUser(UserRole.CUSTOMER));
        String key = "mismatch-" + UUID.randomUUID();

        mvc.perform(idempotentPost(token, key, "{\"value\":\"a\"}")).andExpect(status().isCreated());
        mvc.perform(idempotentPost(token, key, "{\"value\":\"b\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Idempotency key was reused with a different request."));
    }

    @Test
    void failedRequestFreesItsIdempotencyKey() throws Exception {
        String token = bearer(createUser(UserRole.CUSTOMER));
        String key = "retry-" + UUID.randomUUID();

        probe.failNextCall();
        mvc.perform(idempotentPost(token, key, "{\"value\":\"c\"}")).andExpect(status().isBadRequest());
        mvc.perform(idempotentPost(token, key, "{\"value\":\"c\"}")).andExpect(status().isCreated());
    }

    @Test
    void idempotencyKeyIsRequiredAndBounded() throws Exception {
        String token = bearer(createUser(UserRole.CUSTOMER));

        mvc.perform(post("/test/idempotency").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"a\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(idempotentPost(token, "k".repeat(161), "{\"value\":\"a\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentFirstUseOfAnIdempotencyKeyIsAConflictNotAServerError() throws Exception {
        User user = createUser(UserRole.CUSTOMER);
        String key = "race-" + UUID.randomUUID();

        List<Object> outcomes = race(2, () -> idempotencyService.checkOrCreate(key, user.getId().toString(), user.getId().toString(), "same-hash"));

        assertThat(outcomes).filteredOn(IdempotencyService.CachedResponse.class::isInstance).hasSize(1);
        assertThat(outcomes).filteredOn(ApiException.class::isInstance)
                .singleElement()
                .satisfies(error -> assertThat(((ApiException) error).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void hallCreationGeneratesTheSeatLayoutAndRejectsDuplicates() throws Exception {
        String admin = bearer(createUser(UserRole.ADMIN));
        String cinemaId = createCinema(admin);

        mvc.perform(post("/admin/cinemas/" + cinemaId + "/halls").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Too Deep", "type", "STANDARD", "totalRows", 27, "totalColumns", 10))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/admin/cinemas/" + cinemaId + "/halls").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Too Wide", "type", "STANDARD", "totalRows", 5, "totalColumns", 51))))
                .andExpect(status().isBadRequest());

        String hallId = json(mvc.perform(post("/admin/cinemas/" + cinemaId + "/halls").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Hall 1", "type", "STANDARD", "totalRows", 3, "totalColumns", 4,
                                "defaultSeatType", "REGULAR"))))
                .andExpect(status().isCreated())).path("id").asText();

        mvc.perform(get("/admin/halls/" + hallId + "/seats").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[11].rowLabel").value("C"))
                .andExpect(jsonPath("$[11].seatNumber").value(4));

        mvc.perform(post("/admin/halls/" + hallId + "/seats").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("rowLabel", "a", "seatNumber", 1, "seatType", "VIP"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Seat A1 already exists in this hall."));
        mvc.perform(post("/admin/halls/" + hallId + "/seat-layout").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("rows", 1, "columns", 1, "defaultSeatType", "REGULAR"))))
                .andExpect(status().isConflict());
        mvc.perform(post("/admin/halls/" + hallId + "/seat-layout").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("rows", 27, "columns", 1, "defaultSeatType", "REGULAR"))))
                .andExpect(status().isBadRequest());

        String bareHallId = json(mvc.perform(post("/admin/cinemas/" + cinemaId + "/halls").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Hall 2", "type", "STANDARD", "totalRows", 2, "totalColumns", 2))))
                .andExpect(status().isCreated())).path("id").asText();
        mvc.perform(get("/admin/halls/" + bareHallId + "/seats").header("Authorization", admin))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void moviesAndCinemasWithShowtimesCannotBeDeleted() throws Exception {
        String admin = bearer(createUser(UserRole.ADMIN));
        String title = "Platform Feature " + UUID.randomUUID().toString().substring(0, 8);
        String movieId = createMovie(admin, title);
        String cinemaId = createCinema(admin);
        String hallId = json(mvc.perform(post("/admin/cinemas/" + cinemaId + "/halls").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Hall S", "type", "STANDARD", "totalRows", 1, "totalColumns", 1))))
                .andExpect(status().isCreated())).path("id").asText();
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
        jdbc.update("insert into showtimes (movie_id, hall_id, start_time, end_time, base_price, status) values (?, ?, ?, ?, ?, 'SCHEDULED')",
                UUID.fromString(movieId), UUID.fromString(hallId), Timestamp.from(start), Timestamp.from(start.plus(2, ChronoUnit.HOURS)),
                new BigDecimal("10.00"));

        mvc.perform(get("/movies/" + title.toLowerCase().replace(' ', '-')))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(movieId));
        mvc.perform(delete("/admin/movies/" + movieId).header("Authorization", admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Archive it instead")));
        mvc.perform(delete("/admin/cinemas/" + cinemaId).header("Authorization", admin))
                .andExpect(status().isConflict());

        String unusedMovieId = createMovie(admin, "Unused " + UUID.randomUUID());
        mvc.perform(delete("/admin/movies/" + unusedMovieId).header("Authorization", admin)).andExpect(status().isNoContent());
        mvc.perform(delete("/admin/movies/" + unusedMovieId).header("Authorization", admin)).andExpect(status().isNotFound());
        mvc.perform(get("/movies/" + unusedMovieId)).andExpect(status().isNotFound());

        String emptyCinemaId = createCinema(admin);
        mvc.perform(delete("/admin/cinemas/" + emptyCinemaId).header("Authorization", admin)).andExpect(status().isNoContent());
    }

    @Test
    void notificationListIsCappedAtTheLatestHundred() throws Exception {
        User user = createUser(UserRole.CUSTOMER);
        for (int index = 0; index < 105; index++) {
            notificationService.create(user.getId(), "TEST", "Notice " + index, "Message " + index);
        }
        String token = bearer(user);

        mvc.perform(get("/notifications").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(105))
                .andExpect(jsonPath("$.notifications.length()").value(100));

        mvc.perform(post("/notifications/read-all").header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(get("/notifications").header("Authorization", token))
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andExpect(jsonPath("$.notifications[0].read").value(true));
    }

    private String createCinema(String admin) throws Exception {
        return json(mvc.perform(post("/admin/cinemas").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("name", "Platform Cinema", "location", "Downtown", "address", "1 Test Street", "city", "Yangon"))))
                .andExpect(status().isCreated())).path("id").asText();
    }

    private String createMovie(String admin, String title) throws Exception {
        Map<String, Object> movie = Map.of("title", title, "description", "Test", "durationMinutes", 100, "genre", "Drama",
                "language", "English", "rating", "PG", "releaseDate", "2026-01-01", "status", "NOW_SHOWING");
        return json(mvc.perform(post("/admin/movies").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(body(movie)))
                .andExpect(status().isCreated())).path("id").asText();
    }

    private MockHttpServletRequestBuilder idempotentPost(String token, String key, String content) {
        return post("/test/idempotency").header("Authorization", token).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(content);
    }

    private JsonNode login(User user) throws Exception {
        return json(mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", user.getEmail(), "password", PASSWORD))))
                .andExpect(status().isOk()));
    }

    private User createUser(UserRole role) {
        User user = new User();
        user.setName("Platform " + role.name());
        user.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        return users.save(user);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.issue(AuthUser.from(user));
    }

    private String body(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode json(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    private List<Object> race(int threads, Callable<Object> task) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (int index = 0; index < threads; index++) {
                futures.add(executor.submit(() -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    try {
                        return task.call();
                    } catch (ApiException ex) {
                        return ex;
                    }
                }));
            }
            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }

    record ProbeRequest(String value) {
    }

    record ProbeResponse(UUID id, String value, int call) {
    }

    @RestController
    @RequestMapping("/test/idempotency")
    static class IdempotencyProbeController {
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicBoolean failNext = new AtomicBoolean();

        int callCount() {
            return calls.get();
        }

        void failNextCall() {
            failNext.set(true);
        }

        @PostMapping
        @Idempotent
        @ResponseStatus(HttpStatus.CREATED)
        ProbeResponse create(@AuthenticationPrincipal AuthUser user, @RequestBody ProbeRequest request) {
            if (failNext.getAndSet(false)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Probe failure requested.");
            }
            return new ProbeResponse(UUID.randomUUID(), request.value(), calls.incrementAndGet());
        }
    }
}
