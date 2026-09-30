package com.cinema.common;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final List<String> AUTH_PATHS = List.of("/auth/login", "/auth/register", "/auth/refresh");
    private static final String PAYMENT_PREFIX = "/payments/";
    private static final String PAYMENT_WEBHOOK_PATH = "/payments/webhook";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int authLimit;
    private final int paymentLimit;
    private final boolean trustForwardedFor;

    public RateLimitFilter(StringRedisTemplate redis, ObjectMapper objectMapper,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.auth-per-minute:10}") int authLimit,
            @Value("${app.rate-limit.payment-per-minute:30}") int paymentLimit,
            @Value("${app.rate-limit.trust-forwarded-for:false}") boolean trustForwardedFor) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.authLimit = authLimit;
        this.paymentLimit = paymentLimit;
        this.trustForwardedFor = trustForwardedFor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Bucket bucket = bucketFor(request);
        if (bucket == null || !enabled) {
            chain.doFilter(request, response);
            return;
        }
        if (withinLimit(bucket, clientIp(request))) {
            chain.doFilter(request, response);
            return;
        }
        reject(response);
    }

    private Bucket bucketFor(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return null;
        }
        String path = request.getServletPath();
        if (AUTH_PATHS.contains(path)) {
            return new Bucket("auth", authLimit);
        }
        if (path.startsWith(PAYMENT_PREFIX) && !PAYMENT_WEBHOOK_PATH.equals(path)) {
            return new Bucket("payment", paymentLimit);
        }
        return null;
    }

    private boolean withinLimit(Bucket bucket, String clientIp) {
        String key = "ratelimit:%s:%s:%d".formatted(bucket.name(), clientIp, Instant.now().getEpochSecond() / WINDOW.toSeconds());
        try {
            Long count = redis.opsForValue().increment(key);
            if (count == null) {
                return true;
            }
            if (count == 1L) {
                redis.expire(key, WINDOW);
            }
            return count <= bucket.limit();
        } catch (RuntimeException ex) {
            log.warn("Rate limiting is disabled for this request because Redis is unreachable: {}", ex.getMessage());
            return true;
        }
    }

    private String clientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwarded)) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(WINDOW.toSeconds()));
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please wait a minute and try again."));
    }

    private record Bucket(String name, int limit) {
    }
}
