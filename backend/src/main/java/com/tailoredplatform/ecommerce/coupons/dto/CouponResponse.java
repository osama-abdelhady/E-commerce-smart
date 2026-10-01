package com.tailoredplatform.ecommerce.coupons.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponResponse(
        Long id,
        String code,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minimumOrderAmount,
        Integer maxRedemptions,
        Integer maxRedemptionsPerUser,
        Integer redemptionsCount,
        Instant validFrom,
        Instant validUntil,
        boolean isActive
) {
}
