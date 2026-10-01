package com.tailoredplatform.ecommerce.coupons.repository;

import com.tailoredplatform.ecommerce.coupons.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {
    long countByCouponIdAndUserId(Long couponId, Long userId);
}
