package com.cinema.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.cinema.auth.AuthDtos.AuthResponse;
import com.cinema.auth.AuthDtos.LoginRequest;
import com.cinema.auth.AuthDtos.RefreshRequest;
import com.cinema.auth.AuthDtos.RegisterRequest;
import com.cinema.auth.AuthDtos.UserPayload;
import com.cinema.common.ApiException;
import com.cinema.user.User;
import com.cinema.user.UserRepository;
import com.cinema.user.UserRole;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshDays;
    private final String timingEqualizerHash;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder, JwtService jwtService,
            @Value("${app.jwt.refresh-token-days}") long refreshDays) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshDays = refreshDays;
        this.timingEqualizerHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > AuthDtos.MAX_PASSWORD_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "password must be at most " + AuthDtos.MAX_PASSWORD_LENGTH + " bytes");
        }
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw emailTaken();
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.CUSTOMER);
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw emailTaken();
        }
        return issue(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Optional<User> user = users.findByEmailIgnoreCase(normalizeEmail(request.email()));
        boolean passwordMatches = passwordEncoder.matches(request.password(),
                user.map(User::getPasswordHash).orElse(timingEqualizerHash));
        if (user.isEmpty() || !passwordMatches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
        return issue(user.get());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken token = refreshTokens.lockByTokenHash(hash(request.refreshToken()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid."));
        Instant now = Instant.now();
        if (token.getRevokedAt() != null) {
            UUID userId = token.getUser().getId();
            int revoked = refreshTokens.revokeAllActiveForUser(userId, now);
            log.warn("A revoked refresh token was presented for user {}; revoked {} active refresh token(s).", userId, revoked);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is no longer valid. Please sign in again.");
        }
        if (token.getExpiresAt().isBefore(now)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is expired.");
        }
        token.setRevokedAt(now);
        return issue(token.getUser());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokens.findByTokenHash(hash(refreshToken)).ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    private ApiException emailTaken() {
        return new ApiException(HttpStatus.CONFLICT, "Email is already registered.");
    }

    private AuthResponse issue(User user) {
        AuthUser authUser = AuthUser.from(user);
        String rawRefresh = UUID.randomUUID() + "." + UUID.randomUUID();
        RefreshToken refresh = new RefreshToken();
        refresh.setUser(user);
        refresh.setTokenHash(hash(rawRefresh));
        refresh.setExpiresAt(Instant.now().plus(refreshDays, ChronoUnit.DAYS));
        refreshTokens.save(refresh);
        return new AuthResponse(jwtService.issue(authUser), rawRefresh,
                new UserPayload(user.getId(), user.getName(), user.getEmail(), user.getRole().name()));
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash token", ex);
        }
    }
}
