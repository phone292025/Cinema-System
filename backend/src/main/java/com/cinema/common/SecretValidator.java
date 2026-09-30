package com.cinema.common;

import java.util.Locale;

public final class SecretValidator {
    private static final int MIN_SECRET_LENGTH = 32;
    private static final int MIN_DISTINCT_CHARACTERS = 10;
    private static final String[] UNSAFE_MARKERS = {
            "change-me",
            "changeme",
            "change_me",
            "dev-only",
            "local-compose",
            "replace",
            "placeholder",
            "example",
            "your-secret",
            "your_secret",
            "insecure"
    };

    private SecretValidator() {
    }

    public static String requireStrongSecret(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must be configured.");
        }
        if (value.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(propertyName + " must be at least " + MIN_SECRET_LENGTH + " characters.");
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String marker : UNSAFE_MARKERS) {
            if (normalized.contains(marker)) {
                throw new IllegalStateException(propertyName + " contains an unsafe placeholder value.");
            }
        }
        if (value.chars().distinct().count() < MIN_DISTINCT_CHARACTERS) {
            throw new IllegalStateException(propertyName + " is too repetitive; use a randomly generated value.");
        }
        return value;
    }
}
