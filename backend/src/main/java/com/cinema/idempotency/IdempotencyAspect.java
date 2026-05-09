package com.cinema.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import com.cinema.auth.AuthUser;
import com.cinema.common.ApiException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class IdempotencyAspect {
    private final IdempotencyService idempotency;
    private final ObjectMapper objectMapper;

    public IdempotencyAspect(IdempotencyService idempotency, ObjectMapper objectMapper) {
        this.idempotency = idempotency;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(com.cinema.idempotency.Idempotent)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required.");
        }

        AuthUser authUser = findAuthUser(joinPoint.getArgs());
        String actorKey = authUser == null ? "anonymous" : authUser.id().toString();
        String requestHash = hash(request.getMethod() + " " + request.getRequestURI() + " " + objectMapper.writeValueAsString(joinPoint.getArgs()));
        IdempotencyService.CachedResponse cached = idempotency.checkOrCreate(key, actorKey, authUser == null ? null : authUser.id().toString(), requestHash);

        if (cached.hit()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            JavaType returnType = objectMapper.constructType(signature.getMethod().getGenericReturnType());
            return objectMapper.readValue(cached.responseBody(), returnType);
        }

        try {
            Object result = joinPoint.proceed();
            idempotency.storeResponse(key, actorKey, HttpStatus.OK.value(), objectMapper.writeValueAsString(result));
            return result;
        } catch (Throwable ex) {
            idempotency.deletePending(key, actorKey);
            throw ex;
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }

    private AuthUser findAuthUser(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof AuthUser authUser) {
                return authUser;
            }
        }
        return null;
    }

    private String hash(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash idempotency request", ex);
        }
    }
}
