package com.cinema.common;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(Instant timestamp, int status, String error, String message, String requestId) {
    public ErrorResponse(Instant timestamp, int status, String error, String message) {
        this(timestamp, status, error, message, RequestIdFilter.currentRequestId());
    }

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message);
    }
}
