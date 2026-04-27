package com.cinema.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.cinema.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

class JwtServiceTest {
    @Test
    void shouldParseIssuedToken() {
        JwtService service = new JwtService(new ObjectMapper(), "test-secret-test-secret-test-secret", 30);
        AuthUser user = new AuthUser(UUID.randomUUID(), "Demo", "demo@example.com", UserRole.CUSTOMER);

        String token = service.issue(user);

        assertThat(service.parse(token)).contains(user);
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtService service = new JwtService(new ObjectMapper(), "test-secret-test-secret-test-secret", -1);
        AuthUser user = new AuthUser(UUID.randomUUID(), "Demo", "demo@example.com", UserRole.CUSTOMER);

        String token = service.issue(user);

        assertThat(service.parse(token)).isEmpty();
    }
}
