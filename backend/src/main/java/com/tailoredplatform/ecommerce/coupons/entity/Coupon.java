package com.tailoredplatform.ecommerce.coupons.entity;

import com.tailoredplatform.ecommerce.common.SimpleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Coupon extends SimpleEntity {

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "minimum_order_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumOrderAmount = BigDecimal.ZERO;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(name = "max_redemptions_per_user", nullable = false)
    private Integer maxRedemptionsPerUser = 1;

    @Column(name = "redemptions_count", nullable = false)
    private Integer redemptionsCount = 0;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum DiscountType { PERCENTAGE, FIXED_AMOUNT }

    public boolean isCurrentlyValid() {
        Instant now = Instant.now();
        boolean withinWindow = !now.isBefore(validFrom) && !now.isAfter(validUntil);
        boolean underGlobalCap = maxRedemptions == null || redemptionsCount < maxRedemptions;
        return isActive && withinWindow && underGlobalCap;
    }

    public BigDecimal calculateDiscount(BigDecimal orderSubtotal) {
        if (orderSubtotal.compareTo(minimumOrderAmount) < 0) return BigDecimal.ZERO;
        return switch (discountType) {
            case PERCENTAGE -> orderSubtotal.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            case FIXED_AMOUNT -> discountValue.min(orderSubtotal);
        };
    }
}
