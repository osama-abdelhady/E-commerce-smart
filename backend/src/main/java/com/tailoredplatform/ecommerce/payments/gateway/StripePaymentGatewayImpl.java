package com.tailoredplatform.ecommerce.payments.gateway;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Stripe test-mode integration via PaymentIntents. Amounts are converted to
 * the smallest currency unit (cents) as Stripe's API requires. The
 * idempotencyKey passed in is forwarded to Stripe's own idempotency-key
 * mechanism (RequestOptions), so a network retry of the SAME create-intent
 * call is deduplicated by Stripe itself, on top of our own Payment.idempotencyKey
 * uniqueness constraint.
 */
@Service
@Slf4j
public class StripePaymentGatewayImpl implements PaymentGateway {

    @Value("${app.stripe.secret-key}")
    private String secretKey;

    @PostConstruct
    void init() {
        if (secretKey == null || secretKey.isBlank()) {
            log.warn("STRIPE_SECRET_KEY is not set — payment creation will fail until it is configured.");
            return;
        }
        Stripe.apiKey = secretKey;
    }

    @Override
    public PaymentIntentResult createPaymentIntent(BigDecimal amount, String currency, String idempotencyKey, Map<String, String> metadata) {
        try {
            long amountInMinorUnits = amount.setScale(2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .longValueExact();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInMinorUnits)
                    .setCurrency(currency.toLowerCase())
                    .putAllMetadata(metadata)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params, options);
            return new PaymentIntentResult(intent.getId(), intent.getClientSecret());
        } catch (StripeException e) {
            log.error("Stripe PaymentIntent creation failed: {}", e.getMessage());
            throw new BusinessRuleViolationException("Unable to initiate payment. Please try again.");
        }
    }

    @Override
    public RefundResult refund(String providerPaymentId, BigDecimal amount, String reason) {
        try {
            RefundCreateParams.Builder builder = RefundCreateParams.builder()
                    .setPaymentIntent(providerPaymentId);
            if (amount != null) {
                builder.setAmount(amount.setScale(2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .longValueExact());
            }
            Refund refund = Refund.create(builder.build());
            return new RefundResult(refund.getId(), refund.getStatus());
        } catch (StripeException e) {
            log.error("Stripe refund failed for payment intent {}: {}", providerPaymentId, e.getMessage());
            throw new BusinessRuleViolationException("Unable to process refund. Please try again or contact support.");
        }
    }
}
