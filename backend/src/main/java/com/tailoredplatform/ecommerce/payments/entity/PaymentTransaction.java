package com.tailoredplatform.ecommerce.payments.entity;

import com.tailoredplatform.ecommerce.common.SimpleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Append-only. Every Stripe webhook delivery for a payment writes one row
 * here — never mutated or deleted — so a support investigation can
 * reconstruct exactly what the provider told us and when.
 * providerEventId is unique so a re-delivered webhook is a no-op, not a
 * duplicate application of the event.
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class PaymentTransaction extends SimpleEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Lob
    @Column(name = "raw_payload")
    private String rawPayload;

    @EqualsAndHashCode.Include
    @Column(name = "provider_event_id", unique = true)
    private String providerEventId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
