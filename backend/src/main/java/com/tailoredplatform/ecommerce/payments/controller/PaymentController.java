package com.tailoredplatform.ecommerce.payments.controller;

import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import com.tailoredplatform.ecommerce.orders.entity.Order;
import com.tailoredplatform.ecommerce.orders.repository.OrderRepository;
import com.tailoredplatform.ecommerce.payments.dto.PaymentIntentResponse;
import com.tailoredplatform.ecommerce.payments.service.PaymentService;
import com.tailoredplatform.ecommerce.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Stripe PaymentIntent creation and webhook handling")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    @PostMapping("/orders/{orderId}/intent")
    @Operation(summary = "Create (or re-fetch) a Stripe PaymentIntent for a PENDING order")
    public PaymentIntentResponse createIntent(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long orderId) {
        return paymentService.createIntentForOrder(principal.getUser(), orderId);
    }

    /**
     * Public per SecurityConfig's PUBLIC_ENDPOINTS list — Stripe can't send a
     * JWT. Authenticity is established entirely by the signature check
     * inside PaymentService.handleWebhook, not by anything in Spring
     * Security. The raw request body is taken as a plain String specifically
     * because Stripe's signature is computed over the exact bytes received —
     * any JSON re-serialization here would break verification.
     */
    @PostMapping("/webhook")
    @Operation(summary = "Stripe webhook endpoint — signature-verified, not JWT-authenticated")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature
    ) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/orders/{orderId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: refund a paid order via Stripe")
    public ResponseEntity<Void> refund(
            @PathVariable Long orderId,
            @RequestParam(required = false, defaultValue = "Refunded by admin") String reason
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
        paymentService.refundForOrder(order, reason);
        return ResponseEntity.noContent().build();
    }
}
