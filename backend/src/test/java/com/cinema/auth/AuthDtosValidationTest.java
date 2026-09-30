package com.cinema.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import com.cinema.auth.AuthDtos.RegisterRequest;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AuthDtosValidationTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void acceptsValidRegistration() {
        assertThat(invalidFields(new RegisterRequest("Ada", "ada@example.com", "long-enough", "+95 9 000 000"))).isEmpty();
    }

    @Test
    void rejectsNullPassword() {
        assertThat(invalidFields(new RegisterRequest("Ada", "ada@example.com", null, null))).containsExactly("password");
    }

    @Test
    void boundsPasswordBetween8And72Characters() {
        assertThat(invalidFields(new RegisterRequest("Ada", "ada@example.com", "short", null))).containsExactly("password");
        assertThat(invalidFields(new RegisterRequest("Ada", "ada@example.com", "p".repeat(72), null))).isEmpty();
        assertThat(invalidFields(new RegisterRequest("Ada", "ada@example.com", "p".repeat(73), null))).containsExactly("password");
    }

    @Test
    void capsFieldsAtTheirColumnLengths() {
        String longEmail = "a".repeat(64) + "@" + "b".repeat(60) + "." + "c".repeat(60) + "." + "d".repeat(40) + ".com";

        assertThat(invalidFields(new RegisterRequest("n".repeat(160), "ada@example.com", "long-enough", "1".repeat(40)))).isEmpty();
        assertThat(invalidFields(new RegisterRequest("n".repeat(161), "ada@example.com", "long-enough", null))).containsExactly("name");
        assertThat(invalidFields(new RegisterRequest("Ada", longEmail, "long-enough", null))).contains("email");
        assertThat(invalidFields(new RegisterRequest("Ada", "ada@example.com", "long-enough", "1".repeat(41)))).containsExactly("phone");
    }

    private Set<String> invalidFields(RegisterRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
