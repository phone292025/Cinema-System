package com.cinema.common;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cinema.booking.IllegalBookingStateTransitionException;
import com.fasterxml.jackson.databind.JsonMappingException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final AuthenticationTrustResolver TRUST_RESOLVER = new AuthenticationTrustResolverImpl();

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> api(ApiException ex, HttpServletRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("{} {} failed: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        }
        return build(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::format)
                .collect(Collectors.joining("; "));
        String globalErrors = ex.getBindingResult().getGlobalErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        String message = fieldErrors.isEmpty() ? globalErrors : globalErrors.isEmpty() ? fieldErrors : fieldErrors + "; " + globalErrors;
        return build(HttpStatus.BAD_REQUEST, message.isEmpty() ? "Request validation failed." : message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorResponse> methodValidation(HandlerMethodValidationException ex) {
        String message = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName() + " " + error.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message.isEmpty() ? "Request validation failed." : message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> constraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> lastNode(violation.getPropertyPath().toString()) + " " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message.isEmpty() ? "Request validation failed." : message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> missingParameter(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "Required parameter '" + ex.getParameterName() + "' is missing.");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ErrorResponse> missingHeader(MissingRequestHeaderException ex) {
        return build(HttpStatus.BAD_REQUEST, "Required header '" + ex.getHeaderName() + "' is missing.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = UUID.class.equals(ex.getRequiredType())
                ? "Parameter '" + ex.getName() + "' must be a valid UUID."
                : "Parameter '" + ex.getName() + "' has an invalid value.";
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
            String field = mapping.getPath().stream()
                    .map(reference -> reference.getFieldName() != null ? reference.getFieldName() : "[" + reference.getIndex() + "]")
                    .collect(Collectors.joining("."));
            return build(HttpStatus.BAD_REQUEST, "Field '" + field + "' has an invalid value.");
        }
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON.");
    }

    @ExceptionHandler({ NoResourceFoundException.class, NoHandlerFoundException.class })
    ResponseEntity<ErrorResponse> notFound(Exception ex) {
        return build(HttpStatus.NOT_FOUND, "No endpoint matches this path.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        HttpHeaders headers = new HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) {
            headers.setAllow(ex.getSupportedHttpMethods());
        }
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Method " + ex.getMethod() + " is not supported for this path.", headers);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> unsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        HttpHeaders headers = new HttpHeaders();
        if (!ex.getSupportedMediaTypes().isEmpty()) {
            headers.setAccept(ex.getSupportedMediaTypes());
        }
        String contentType = ex.getContentType() == null ? "missing" : "'" + ex.getContentType() + "'";
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type " + contentType + " is not supported.", headers);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<Void> notAcceptable(HttpMediaTypeNotAcceptableException ex) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> denied(AccessDeniedException ex) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || TRUST_RESOLVER.isAnonymous(authentication)) {
            return build(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return build(HttpStatus.FORBIDDEN, "You do not have access to this resource.");
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> unauthenticated(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication is required.");
    }

    @ExceptionHandler(IllegalBookingStateTransitionException.class)
    ResponseEntity<ErrorResponse> illegalBookingTransition(IllegalBookingStateTransitionException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ErrorResponse> optimisticLock(OptimisticLockingFailureException ex) {
        return build(HttpStatus.CONFLICT, "The seat changed while this request was being processed. Please refresh and try again.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> dataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("{} {} violated a database constraint: {}", request.getMethod(), request.getRequestURI(),
                NestedExceptionUtils.getMostSpecificCause(ex).getMessage());
        return build(HttpStatus.CONFLICT, "The request conflicts with existing data.");
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    void clientDisconnected(AsyncRequestNotUsableException ex) {
        log.debug("Client disconnected before the response was written: {}", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof org.springframework.web.ErrorResponse framework && !framework.getStatusCode().is5xxServerError()) {
            log.warn("{} {} rejected: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
            String detail = framework.getBody().getDetail();
            return build(framework.getStatusCode(), detail == null ? "The request could not be processed." : detail);
        }
        log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatusCode status, String message) {
        return build(status, message, new HttpHeaders());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatusCode status, String message, HttpHeaders headers) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String reason = resolved == null ? "Error" : resolved.getReasonPhrase();
        return ResponseEntity.status(status)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(Instant.now(), status.value(), reason, message));
    }

    private String format(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }

    private String lastNode(String propertyPath) {
        int dot = propertyPath.lastIndexOf('.');
        return dot < 0 ? propertyPath : propertyPath.substring(dot + 1);
    }
}
