package com.tailoredplatform.ecommerce.payments.gateway;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Every call site in this module talks to this interface, never to Stripe
 * classes directly — swapping providers later (or adding a second one) is a
 * new implementation + a Spring profile/bean choice, not a rewrite of
 * PaymentService or PaymentController. See notifications.service.EmailService
 * for the same pattern applied to email.
 */
public interface PaymentGateway {

    PaymentIntentResult createPaymentIntent(BigDecimal amount, String currency, String idempotencyKey, Map<String, String> metadata);

    RefundResult refund(String providerPaymentId, BigDecimal amount, String reason);

    record PaymentIntentResult(String providerPaymentId, String clientSecret) {}

    record RefundResult(String providerRefundId, String status) {}
}
