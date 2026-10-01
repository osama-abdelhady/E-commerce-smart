package com.tailoredplatform.ecommerce.payments.repository;

import com.tailoredplatform.ecommerce.payments.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByProviderPaymentId(String providerPaymentId);
    List<Payment> findByOrderId(Long orderId);
}
