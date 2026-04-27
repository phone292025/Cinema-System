package com.cinema.user;

import java.util.UUID;

public record UserProfileResponse(UUID id, String name, String email, String role) {
}
