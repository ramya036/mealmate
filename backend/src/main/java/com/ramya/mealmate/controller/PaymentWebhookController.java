package com.ramya.mealmate.controller;

import com.ramya.mealmate.payment.PaymentGateway;
import com.ramya.mealmate.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Stripe calls this endpoint when a payment succeeds or fails. The order is only marked PAID here,
 * after the signature is verified, never on the client's word.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {

    private final PaymentGateway payments;
    private final OrderService orders;

    public PaymentWebhookController(PaymentGateway payments, OrderService orders) {
        this.payments = payments;
        this.orders = orders;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                        @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        return payments.parseWebhook(payload, signature)
                .map(event -> {
                    orders.handlePaymentEvent(event);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }
}
