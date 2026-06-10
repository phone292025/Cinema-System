package com.cinema.common;

import java.util.Locale;

public final class SecretValidator {
    private static final int MIN_SECRET_LENGTH = 32;
    private static final String[] UNSAFE_MARKERS = {
            "change-me",
            "dev-only",
            "local-compose"
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
        return value;
    }
}
