package com.tailoredplatform.ecommerce.payments.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import com.tailoredplatform.ecommerce.orders.entity.Order;
import com.tailoredplatform.ecommerce.orders.repository.OrderRepository;
import com.tailoredplatform.ecommerce.orders.service.OrderService;
import com.tailoredplatform.ecommerce.payments.dto.PaymentIntentResponse;
import com.tailoredplatform.ecommerce.payments.entity.Payment;
import com.tailoredplatform.ecommerce.payments.entity.PaymentTransaction;
import com.tailoredplatform.ecommerce.payments.gateway.PaymentGateway;
import com.tailoredplatform.ecommerce.payments.repository.PaymentRepository;
import com.tailoredplatform.ecommerce.payments.repository.PaymentTransactionRepository;
import com.tailoredplatform.ecommerce.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    @Value("${app.stripe.webhook-secret}")
    private String webhookSecret;

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final OrderService orderService;

    @Transactional
    public PaymentIntentResponse createIntentForOrder(User user, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        if (order.getStatus() != Order.Status.PENDING) {
            throw new BusinessRuleViolationException("This order is not awaiting payment.");
        }

        // Re-issuing an intent for the same order reuses the same
        // idempotency key, so retried "pay now" clicks don't create
        // multiple PaymentIntents at Stripe for one order.
        String idempotencyKey = "order-" + order.getId() + "-intent";
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            // clientSecret is intentionally null on a re-fetch — Stripe's
            // client secret for an existing intent isn't re-exposed here to
            // avoid a stale secret being reused after the intent already
            // resolved; the frontend should reuse its originally stored one
            // within the same checkout session.
            return new PaymentIntentResponse(null, payment.getAmount(), payment.getCurrency(), order.getOrderNumber());
        }

        var intent = paymentGateway.createPaymentIntent(
                order.getGrandTotal(), order.getCurrency(), idempotencyKey,
                Map.of("orderId", String.valueOf(order.getId()), "orderNumber", order.getOrderNumber())
        );

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setProvider(Payment.Provider.STRIPE);
        payment.setProviderPaymentId(intent.providerPaymentId());
        payment.setStatus(Payment.Status.INITIATED);
        payment.setAmount(order.getGrandTotal());
        payment.setCurrency(order.getCurrency());
        payment.setIdempotencyKey(idempotencyKey);
        paymentRepository.save(payment);

        return new PaymentIntentResponse(intent.clientSecret(), order.getGrandTotal(), order.getCurrency(), order.getOrderNumber());
    }

    /**
     * Verifies the Stripe signature FIRST — nothing here is trusted until
     * Webhook.constructEvent succeeds. This is the only path that ever moves
     * a Payment to SUCCEEDED; a frontend "success" redirect is never treated
     * as proof of payment (see the spec's explicit requirement on this).
     */
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Rejected webhook with invalid Stripe signature.");
            throw new BusinessRuleViolationException("Invalid webhook signature.");
        }

        // Re-delivered webhooks are a no-op: provider_event_id is unique,
        // so a second delivery of the same event is simply skipped here
        // rather than double-applying it.
        if (paymentTransactionRepository.existsByProviderEventId(event.getId())) {
            log.info("Skipping already-processed Stripe event {}", event.getId());
            return;
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isEmpty() || !(deserializer.getObject().get() instanceof PaymentIntent intent)) {
            log.info("Ignoring Stripe event {} of type {} — not a PaymentIntent payload.", event.getId(), event.getType());
            return;
        }

        Optional<Payment> paymentOpt = paymentRepository.findByProviderPaymentId(intent.getId());
        if (paymentOpt.isEmpty()) {
            log.warn("Received Stripe event for unknown PaymentIntent {}", intent.getId());
            return;
        }
        Payment payment = paymentOpt.get();

        recordTransaction(payment, event.getId(), event.getType(), payload);

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                payment.setStatus(Payment.Status.SUCCEEDED);
                paymentRepository.save(payment);
                orderService.confirmPayment(payment.getOrder().getId());
            }
            case "payment_intent.payment_failed" -> {
                payment.setStatus(Payment.Status.FAILED);
                payment.setFailureReason(extractFailureReason(intent));
                paymentRepository.save(payment);
                orderService.releaseForFailedPayment(payment.getOrder().getId(), payment.getFailureReason());
            }
            default -> log.info("Unhandled Stripe event type: {}", event.getType());
        }
    }

    /** Called from OrderService.cancel() for an order whose payment already succeeded. */
    @Transactional
    public void refundForOrder(Order order, String reason) {
        Payment payment = paymentRepository.findByOrderId(order.getId()).stream()
                .filter(p -> p.getStatus() == Payment.Status.SUCCEEDED)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException("No successful payment found for this order to refund."));

        var refund = paymentGateway.refund(payment.getProviderPaymentId(), payment.getAmount(), reason);

        payment.setStatus(Payment.Status.REFUNDED);
        paymentRepository.save(payment);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setPayment(payment);
        tx.setEventType("refund.created");
        tx.setProviderEventId(refund.providerRefundId());
        tx.setCreatedAt(Instant.now());
        paymentTransactionRepository.save(tx);
    }

    private void recordTransaction(Payment payment, String eventId, String eventType, String rawPayload) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setPayment(payment);
        tx.setEventType(eventType);
        tx.setProviderEventId(eventId);
        tx.setRawPayload(rawPayload);
        tx.setCreatedAt(Instant.now());
        paymentTransactionRepository.save(tx);
    }

    private String extractFailureReason(PaymentIntent intent) {
        if (intent.getLastPaymentError() != null) {
            return intent.getLastPaymentError().getMessage();
        }
        return "Payment failed.";
    }
}
