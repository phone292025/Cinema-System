package com.cinema.auth;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.cinema.common.SecretValidator;
import com.cinema.user.UserRole;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final String ALGORITHM = "HS256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long accessTokenSeconds;

    public JwtService(ObjectMapper objectMapper,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-minutes}") long accessTokenMinutes) {
        this.objectMapper = objectMapper;
        this.secret = SecretValidator.requireStrongSecret(secret, "app.jwt.secret").getBytes(StandardCharsets.UTF_8);
        this.accessTokenSeconds = accessTokenMinutes * 60;
    }

    public String issue(AuthUser user) {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of("alg", ALGORITHM, "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.id().toString());
        payload.put("name", user.name());
        payload.put("email", user.email());
        payload.put("role", user.role().name());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(accessTokenSeconds).getEpochSecond());
        return sign(header, payload);
    }

    public Optional<AuthUser> parse(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) {
                return Optional.empty();
            }
            byte[] expected = URL_ENCODER.encode(hmac(parts[0] + "." + parts[1]));
            if (!MessageDigest.isEqual(expected, parts[2].getBytes(StandardCharsets.US_ASCII))) {
                return Optional.empty();
            }
            Map<String, Object> header = objectMapper.readValue(URL_DECODER.decode(parts[0]), new TypeReference<>() {
            });
            if (!ALGORITHM.equals(header.get("alg"))) {
                return Optional.empty();
            }
            Map<String, Object> payload = objectMapper.readValue(URL_DECODER.decode(parts[1]), new TypeReference<>() {
            });
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                return Optional.empty();
            }
            return Optional.of(new AuthUser(
                    UUID.fromString((String) payload.get("sub")),
                    (String) payload.get("name"),
                    (String) payload.get("email"),
                    UserRole.valueOf((String) payload.get("role"))));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String sign(Map<String, Object> header, Map<String, Object> payload) {
        try {
            String encodedHeader = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(header));
            String encodedPayload = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            String input = encodedHeader + "." + encodedPayload;
            return input + "." + URL_ENCODER.encodeToString(hmac(input));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to issue token", ex);
        }
    }

    private byte[] hmac(String input) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
    }
}
