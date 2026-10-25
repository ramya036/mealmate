package com.ramya.mealmate.payment;

import com.ramya.mealmate.exception.ApiException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class StripePaymentGateway implements PaymentGateway {

    private final String secretKey;
    private final String webhookSecret;

    public StripePaymentGateway(@Value("${mealmate.stripe.secret-key:}") String secretKey,
                                @Value("${mealmate.stripe.webhook-secret:}") String webhookSecret) {
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public Intent createIntent(long amountCents, String currency, Long orderId, String idempotencyKey) {
        if (secretKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Payments are not configured (set STRIPE_SECRET_KEY)");
        }
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency)
                .putMetadata("orderId", String.valueOf(orderId))
                .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true).build())
                .build();
        // Idempotency key: retrying checkout for the same order never creates a second charge.
        RequestOptions options = RequestOptions.builder()
                .setApiKey(secretKey)
                .setIdempotencyKey(idempotencyKey)
                .build();
        try {
            PaymentIntent pi = PaymentIntent.create(params, options);
            return new Intent(pi.getId(), pi.getClientSecret());
        } catch (StripeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Payment provider error: " + e.getMessage());
        }
    }

    @Override
    public Optional<PaymentEvent> parseWebhook(String payload, String signatureHeader) {
        if (webhookSecret.isBlank() || signatureHeader == null) {
            return Optional.empty();
        }
        try {
            Event event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
            return event.getDataObjectDeserializer().getObject()
                    .filter(PaymentIntent.class::isInstance)
                    .map(o -> new PaymentEvent(event.getType(), ((PaymentIntent) o).getId()));
        } catch (SignatureVerificationException | RuntimeException e) {
            return Optional.empty(); // bad signature or malformed payload
        }
    }
}
