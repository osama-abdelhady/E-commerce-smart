package com.tailoredplatform.ecommerce.coupons.dto;

import com.tailoredplatform.ecommerce.coupons.entity.Coupon;

import java.math.BigDecimal;

public record CouponValidationResult(
        Coupon coupon,
        BigDecimal discountAmount
) {
}
