package com.cinema.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.cinema.auth.AuthDtos.LoginRequest;
import com.cinema.auth.AuthDtos.RefreshRequest;
import com.cinema.auth.AuthDtos.RegisterRequest;
import com.cinema.common.ApiException;
import com.cinema.user.User;
import com.cinema.user.UserRepository;
import com.cinema.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {
    private static final String DUMMY_HASH = "$2a$10$dummyhashdummyhashdummyhashdummyhashdummyhashdummyha";

    private UserRepository users;
    private RefreshTokenRepository refreshTokens;
    private PasswordEncoder passwordEncoder;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        refreshTokens = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn(DUMMY_HASH);
        JwtService jwtService = new JwtService(new ObjectMapper(), "auth-service-test-7Hq2Lx9Vn4Kp8Rz3-auth", 30);
        service = new AuthService(users, refreshTokens, passwordEncoder, jwtService, 14);
    }

    @Test
    void loginForUnknownEmailStillRunsPasswordHashComparison() {
        when(users.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ghost@example.com", "whatever-password")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(passwordEncoder).matches("whatever-password", DUMMY_HASH);
    }

    @Test
    void loginWithWrongPasswordIsRejectedWithTheSameMessage() {
        User user = user("known@example.com");
        when(users.findByEmailIgnoreCase("known@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("known@example.com", "wrong-password")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    void loginNormalizesEmail() {
        User user = user("known@example.com");
        when(users.findByEmailIgnoreCase("known@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("right-password", user.getPasswordHash())).thenReturn(true);

        assertThat(service.login(new LoginRequest("  Known@Example.COM ", "right-password")).user().email()).isEqualTo("known@example.com");
    }

    @Test
    void registerNormalizesEmailAndFlushesImmediately() {
        when(users.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        service.register(new RegisterRequest("  New Person ", " New@Example.com ", "long-enough-password", " "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getValue().getName()).isEqualTo("New Person");
        assertThat(saved.getValue().getPhone()).isNull();
        assertThat(saved.getValue().getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void registerRejectsKnownEmailWithConflict() {
        when(users.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("Taken", "TAKEN@example.com", "long-enough-password", null)))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void registerRaceOnUniqueEmailBecomesConflict() {
        when(users.existsByEmailIgnoreCase("race@example.com")).thenReturn(false);
        when(users.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.register(new RegisterRequest("Race", "race@example.com", "long-enough-password", null)))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getMessage()).isEqualTo("Email is already registered.");
                });
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void registerRejectsPasswordsLongerThan72Bytes() {
        String multiByte = "é".repeat(40);

        assertThatThrownBy(() -> service.register(new RegisterRequest("Bytes", "bytes@example.com", multiByte, null)))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(passwordEncoder, never()).encode(multiByte);
    }

    @Test
    void refreshRotatesAnActiveToken() {
        RefreshToken token = token(user("rotate@example.com"), null, Instant.now().plusSeconds(3600));
        when(refreshTokens.lockByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThat(service.refresh(new RefreshRequest("raw-token")).refreshToken()).isNotBlank();
        assertThat(token.getRevokedAt()).isNotNull();
        verify(refreshTokens, never()).revokeAllActiveForUser(any(), any());
    }

    @Test
    void reusingRevokedTokenRevokesEveryActiveTokenOfThatUser() {
        User user = user("reuse@example.com");
        RefreshToken token = token(user, Instant.now().minusSeconds(5), Instant.now().plusSeconds(3600));
        when(refreshTokens.lockByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.refresh(new RefreshRequest("raw-token")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(refreshTokens).revokeAllActiveForUser(eq(user.getId()), any(Instant.class));
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void expiredTokenIsRejectedWithoutRevokingTheFamily() {
        RefreshToken token = token(user("expired@example.com"), null, Instant.now().minusSeconds(1));
        when(refreshTokens.lockByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.refresh(new RefreshRequest("raw-token")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(refreshTokens, never()).revokeAllActiveForUser(any(), any());
    }

    @Test
    void unknownRefreshTokenIsRejected() {
        when(refreshTokens.lockByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(new RefreshRequest("raw-token")))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    private User user(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Test User");
        user.setEmail(email);
        user.setPasswordHash("$2a$10$storedhashstoredhashstoredhashstoredhashstoredhashst");
        user.setRole(UserRole.CUSTOMER);
        return user;
    }

    private RefreshToken token(User user, Instant revokedAt, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setRevokedAt(revokedAt);
        token.setExpiresAt(expiresAt);
        return token;
    }
}
