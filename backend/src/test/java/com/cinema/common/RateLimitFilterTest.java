package com.cinema.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    void allowsRequestsWithinTheLimit() throws Exception {
        when(values.increment(anyString())).thenReturn(3L);

        MockHttpServletResponse response = run(filter(false), post("/auth/login", "10.0.0.1"));

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void blocksRequestsOverTheLimitWithJsonError() throws Exception {
        when(values.increment(anyString())).thenReturn(11L);

        MockHttpServletResponse response = run(filter(false), post("/auth/login", "10.0.0.1"));

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentAsString()).contains("\"status\":429").contains("Too many requests");
    }

    @Test
    void failsOpenWhenRedisIsUnavailable() throws Exception {
        when(values.increment(anyString())).thenThrow(new RedisConnectionFailureException("down"));

        MockHttpServletResponse response = run(filter(false), post("/auth/register", "10.0.0.1"));

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void ignoresForwardedForUnlessTrusted() throws Exception {
        when(values.increment(anyString())).thenReturn(1L);
        MockHttpServletRequest request = post("/auth/login", "10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");

        run(filter(false), request);

        assertThat(countedKey()).contains(":10.0.0.1:").doesNotContain("203.0.113.9");
    }

    @Test
    void usesFirstForwardedForAddressWhenTrusted() throws Exception {
        when(values.increment(anyString())).thenReturn(1L);
        MockHttpServletRequest request = post("/auth/login", "10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");

        run(filter(true), request);

        assertThat(countedKey()).contains(":203.0.113.9:");
    }

    @Test
    void countsPaymentEndpointsInTheirOwnBucket() throws Exception {
        when(values.increment(anyString())).thenReturn(31L);

        MockHttpServletResponse response = run(filter(false), post("/payments/mock-callback", "10.0.0.1"));

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(countedKey()).startsWith("ratelimit:payment:");
    }

    @Test
    void exemptsThePaymentWebhook() throws Exception {
        MockHttpServletResponse response = run(filter(false), post("/payments/webhook", "10.0.0.1"));

        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(redis);
    }

    @Test
    void ignoresReadsAndUnlimitedPaths() throws Exception {
        MockHttpServletRequest get = new MockHttpServletRequest("GET", "/auth/login");
        get.setServletPath("/auth/login");

        assertThat(run(filter(false), get).getStatus()).isEqualTo(200);
        assertThat(run(filter(false), post("/bookings", "10.0.0.1")).getStatus()).isEqualTo(200);
        verifyNoInteractions(redis);
    }

    private RateLimitFilter filter(boolean trustForwardedFor) {
        return new RateLimitFilter(redis, objectMapper, true, 10, 30, trustForwardedFor);
    }

    private MockHttpServletRequest post(String path, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private MockHttpServletResponse run(RateLimitFilter filter, MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private String countedKey() {
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(values).increment(key.capture());
        return key.getValue();
    }
}
