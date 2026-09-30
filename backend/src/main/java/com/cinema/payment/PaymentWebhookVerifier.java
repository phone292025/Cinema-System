package com.cinema.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.cinema.common.ApiException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentWebhookVerifier {
    static final String SIGNATURE_PREFIX = "sha256=";

    private final byte[] secret;

    public PaymentWebhookVerifier(@Value("${app.payments.webhook-secret:}") String secret) {
        this.secret = secret == null || secret.isBlank() ? null : secret.getBytes(StandardCharsets.UTF_8);
    }

    public void verify(byte[] body, String signatureHeader) {
        if (secret == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "The payment webhook is not configured.");
        }
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing payment signature.");
        }
        byte[] expected = sign(body).getBytes(StandardCharsets.US_ASCII);
        byte[] presented = signatureHeader.substring(SIGNATURE_PREFIX.length()).trim().toLowerCase(Locale.ROOT)
                .getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, presented)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid payment signature.");
        }
    }

    String sign(byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to verify payment signature", ex);
        }
    }
}
