package com.tailoredplatform.ecommerce.orders.entity;

import com.tailoredplatform.ecommerce.common.BaseEntity;
import com.tailoredplatform.ecommerce.coupons.entity.Coupon;
import com.tailoredplatform.ecommerce.users.entity.Address;
import com.tailoredplatform.ecommerce.users.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Order extends BaseEntity {

    @EqualsAndHashCode.Include
    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "tax_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_address_id", nullable = false)
    private Address shippingAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_address_id", nullable = false)
    private Address billingAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt = Instant.now();

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public enum Status { PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED }

    /**
     * Valid forward transitions. Enforced in OrderService — an update that
     * isn't in this map (e.g. DELIVERED -> PROCESSING) is rejected with a
     * BusinessRuleViolationException, never silently applied.
     */
    private static final Map<Status, Set<Status>> VALID_TRANSITIONS = Map.of(
            Status.PENDING, EnumSet.of(Status.CONFIRMED, Status.CANCELLED),
            Status.CONFIRMED, EnumSet.of(Status.PROCESSING, Status.CANCELLED),
            Status.PROCESSING, EnumSet.of(Status.SHIPPED, Status.CANCELLED),
            Status.SHIPPED, EnumSet.of(Status.DELIVERED),
            Status.DELIVERED, EnumSet.of(Status.REFUNDED),
            Status.CANCELLED, EnumSet.noneOf(Status.class),
            Status.REFUNDED, EnumSet.noneOf(Status.class)
    );

    public boolean canTransitionTo(Status target) {
        return VALID_TRANSITIONS.getOrDefault(this.status, EnumSet.noneOf(Status.class)).contains(target);
    }

    public boolean isCancellable() {
        return status == Status.PENDING || status == Status.CONFIRMED;
    }
}
