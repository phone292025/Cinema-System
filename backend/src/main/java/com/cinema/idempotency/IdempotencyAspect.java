package com.cinema.idempotency;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import com.cinema.auth.AuthUser;
import com.cinema.common.ApiException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class IdempotencyAspect {
    static final String HEADER = "Idempotency-Key";
    static final int MAX_KEY_LENGTH = 160;

    private final IdempotencyService idempotency;
    private final ObjectMapper objectMapper;

    public IdempotencyAspect(IdempotencyService idempotency, ObjectMapper objectMapper) {
        this.idempotency = idempotency;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(com.cinema.idempotency.Idempotent)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required.");
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Idempotency-Key must be at most " + MAX_KEY_LENGTH + " characters.");
        }

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        AuthUser authUser = findAuthUser(joinPoint.getArgs());
        String actorKey = authUser == null ? "anonymous" : authUser.id().toString();
        String requestHash = hash(request.getMethod() + " " + request.getRequestURI() + " " + objectMapper.writeValueAsString(joinPoint.getArgs()));
        IdempotencyService.CachedResponse cached = idempotency.checkOrCreate(key, actorKey, authUser == null ? null : authUser.id().toString(), requestHash);

        if (cached.hit()) {
            HttpServletResponse response = attributes.getResponse();
            if (response != null && cached.statusCode() != null) {
                response.setStatus(cached.statusCode());
            }
            JavaType returnType = objectMapper.constructType(method.getGenericReturnType());
            return objectMapper.readValue(cached.responseBody(), returnType);
        }

        try {
            Object result = joinPoint.proceed();
            idempotency.storeResponse(key, actorKey, successStatus(method), objectMapper.writeValueAsString(result));
            return result;
        } catch (Throwable ex) {
            idempotency.deletePending(key, actorKey);
            throw ex;
        }
    }

    private int successStatus(Method method) {
        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(method, ResponseStatus.class);
        if (responseStatus == null) {
            responseStatus = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), ResponseStatus.class);
        }
        return responseStatus == null ? HttpStatus.OK.value() : responseStatus.code().value();
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
