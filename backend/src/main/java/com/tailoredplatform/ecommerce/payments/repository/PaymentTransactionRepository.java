package com.tailoredplatform.ecommerce.payments.repository;

import com.tailoredplatform.ecommerce.payments.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    boolean existsByProviderEventId(String providerEventId);
    Optional<PaymentTransaction> findByProviderEventId(String providerEventId);
}
