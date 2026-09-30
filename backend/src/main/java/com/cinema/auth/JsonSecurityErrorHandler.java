package com.cinema.auth;

import java.io.IOException;

import com.cinema.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public JsonSecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        boolean rejectedToken = Boolean.TRUE.equals(request.getAttribute(JwtAuthenticationFilter.INVALID_TOKEN_ATTRIBUTE));
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, rejectedToken ? "Bearer error=\"invalid_token\"" : "Bearer");
        write(response, HttpStatus.UNAUTHORIZED, rejectedToken
                ? "The access token is invalid or has expired."
                : "Authentication is required.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        write(response, HttpStatus.FORBIDDEN, "You do not have access to this resource.");
    }

    private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(status, message));
    }
}
