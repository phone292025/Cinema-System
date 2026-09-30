package com.cinema.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SecretValidatorTest {
    @Test
    void acceptsRandomLookingSecret() {
        String secret = "u7Qk2mX9pL4vR8sT1wZ6yB3nC5dF0gHj";

        assertThat(SecretValidator.requireStrongSecret(secret, "app.jwt.secret")).isEqualTo(secret);
    }

    @Test
    void rejectsMissingSecret() {
        assertThatThrownBy(() -> SecretValidator.requireStrongSecret(null, "app.jwt.secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be configured");
        assertThatThrownBy(() -> SecretValidator.requireStrongSecret("   ", "app.jwt.secret"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> SecretValidator.requireStrongSecret("u7Qk2mX9pL4vR8sT", "app.jwt.secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "replace-with-at-least-48-random-characters",
            "PLACEHOLDER-u7Qk2mX9pL4vR8sT1wZ6yB3n",
            "example-secret-u7Qk2mX9pL4vR8sT1wZ6y",
            "changeme-u7Qk2mX9pL4vR8sT1wZ6yB3nC5d",
            "change-me-u7Qk2mX9pL4vR8sT1wZ6yB3nC5",
            "dev-only-u7Qk2mX9pL4vR8sT1wZ6yB3nC5d",
            "local-compose-u7Qk2mX9pL4vR8sT1wZ6yB"
    })
    void rejectsPlaceholderMarkers(String secret) {
        assertThatThrownBy(() -> SecretValidator.requireStrongSecret(secret, "app.jwt.secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unsafe placeholder");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "abababababababababababababababababababab",
            "test-secret-test-secret-test-secret"
    })
    void rejectsLowVarietySecrets(String secret) {
        assertThatThrownBy(() -> SecretValidator.requireStrongSecret(secret, "app.jwt.secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too repetitive");
    }
}
