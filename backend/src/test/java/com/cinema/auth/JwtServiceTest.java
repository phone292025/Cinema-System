package com.cinema.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.cinema.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static final String SECRET = "jwt-unit-test-Kq8vN2xLp4RzT7wY-jwt-unit-test";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final AuthUser user = new AuthUser(UUID.randomUUID(), "Demo", "demo@example.com", UserRole.CUSTOMER);

    @Test
    void shouldParseIssuedToken() {
        JwtService service = new JwtService(new ObjectMapper(), SECRET, 30);

        assertThat(service.parse(service.issue(user))).contains(user);
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtService service = new JwtService(new ObjectMapper(), SECRET, -1);

        assertThat(service.parse(service.issue(user))).isEmpty();
    }

    @Test
    void shouldRejectTamperedPayload() {
        JwtService service = new JwtService(new ObjectMapper(), SECRET, 30);
        String[] parts = service.issue(user).split("\\.");
        String adminPayload = URL_ENCODER.encodeToString(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace("CUSTOMER", "ADMIN").getBytes(StandardCharsets.UTF_8));

        assertThat(service.parse(parts[0] + "." + adminPayload + "." + parts[2])).isEmpty();
    }

    @Test
    void shouldRejectTokenSignedWithAnotherSecret() {
        JwtService issuer = new JwtService(new ObjectMapper(), "another-secret-9fH3kLm2Qp7Zx5Vb-another", 30);
        JwtService verifier = new JwtService(new ObjectMapper(), SECRET, 30);

        assertThat(verifier.parse(issuer.issue(user))).isEmpty();
    }

    @Test
    void shouldRejectUnsignedAndNonHs256Tokens() throws Exception {
        JwtService service = new JwtService(new ObjectMapper(), SECRET, 30);
        String payload = service.issue(user).split("\\.")[1];
        String noneHeader = URL_ENCODER.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String hs512Header = URL_ENCODER.encodeToString("{\"alg\":\"HS512\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

        assertThat(service.parse(noneHeader + "." + payload + ".")).isEmpty();
        assertThat(service.parse(hs512Header + "." + payload + "." + sign(hs512Header + "." + payload))).isEmpty();
    }

    @Test
    void shouldRejectGarbage() {
        JwtService service = new JwtService(new ObjectMapper(), SECRET, 30);

        assertThat(service.parse("not-a-token")).isEmpty();
        assertThat(service.parse("a.b.c")).isEmpty();
        assertThat(service.parse("")).isEmpty();
        assertThat(service.parse(service.issue(user) + "x")).isEmpty();
    }

    private String sign(String input) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return URL_ENCODER.encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
    }
}
