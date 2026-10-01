package com.tailoredplatform.ecommerce.coupons.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponRequest(
        @NotBlank String code,
        @NotBlank String discountType,
        @NotNull @DecimalMin("0.01") BigDecimal discountValue,
        @DecimalMin("0.0") BigDecimal minimumOrderAmount,
        Integer maxRedemptions,
        @NotNull Integer maxRedemptionsPerUser,
        @NotNull Instant validFrom,
        @NotNull Instant validUntil,
        boolean isActive
) {
}
