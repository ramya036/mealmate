package com.ramya.mealmate.payment;

import java.util.Optional;

/** Abstraction over the payment provider so business logic and tests don't depend on Stripe directly. */
public interface PaymentGateway {

    record Intent(String id, String clientSecret) {
    }

    record PaymentEvent(String type, String paymentIntentId) {
    }

    Intent createIntent(long amountCents, String currency, Long orderId, String idempotencyKey);

    /** Verifies the webhook signature and returns the event, or empty if the signature is invalid. */
    Optional<PaymentEvent> parseWebhook(String payload, String signatureHeader);
}
