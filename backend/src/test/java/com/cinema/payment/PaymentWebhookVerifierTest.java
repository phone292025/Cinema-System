package com.cinema.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.cinema.common.ApiException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PaymentWebhookVerifierTest {
    private final byte[] body = "{\"paymentReference\":\"PAY-1\",\"status\":\"SUCCEEDED\"}".getBytes(StandardCharsets.UTF_8);
    private final PaymentWebhookVerifier verifier = new PaymentWebhookVerifier("webhook-secret-for-tests");

    @Test
    void acceptsAValidSignatureInEitherCase() {
        String signature = verifier.sign(body);

        assertThatCode(() -> verifier.verify(body, "sha256=" + signature)).doesNotThrowAnyException();
        assertThatCode(() -> verifier.verify(body, "sha256=" + signature.toUpperCase(Locale.ROOT))).doesNotThrowAnyException();
    }

    @Test
    void rejectsATamperedBody() {
        String signature = verifier.sign(body);
        byte[] tampered = "{\"paymentReference\":\"PAY-2\",\"status\":\"SUCCEEDED\"}".getBytes(StandardCharsets.UTF_8);

        assertStatus(() -> verifier.verify(tampered, "sha256=" + signature), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsAMissingOrMalformedHeader() {
        assertStatus(() -> verifier.verify(body, null), HttpStatus.UNAUTHORIZED);
        assertStatus(() -> verifier.verify(body, verifier.sign(body)), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void isNotFoundWhenNoSecretIsConfigured() {
        PaymentWebhookVerifier disabled = new PaymentWebhookVerifier("  ");

        assertStatus(() -> disabled.verify(body, "sha256=abc"), HttpStatus.NOT_FOUND);
    }

    private void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(status));
    }
}
