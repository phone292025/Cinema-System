package com.cinema.auth;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    public static final int MAX_PASSWORD_LENGTH = 72;

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 160) String name,
            @Email @NotBlank @Size(max = 220) String email,
            @NotNull @Size(min = 8, max = MAX_PASSWORD_LENGTH, message = "must be 8 to 72 characters") String password,
            @Size(max = 40) String phone) {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(String accessToken, String refreshToken, UserPayload user) {
    }

    public record UserPayload(UUID id, String name, String email, String role) {
    }
}
