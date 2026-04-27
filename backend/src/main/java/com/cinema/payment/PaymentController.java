package com.cinema.payment;

import com.cinema.auth.AuthUser;
import com.cinema.payment.PaymentDtos.InitiatePaymentRequest;
import com.cinema.payment.PaymentDtos.MockCallbackRequest;
import com.cinema.payment.PaymentDtos.PaymentResponse;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    PaymentResponse initiate(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody InitiatePaymentRequest request) {
        return paymentService.initiate(user, request);
    }

    @PostMapping("/mock-callback")
    PaymentResponse mockCallback(@Valid @RequestBody MockCallbackRequest request) {
        return paymentService.mockCallback(request);
    }
}
