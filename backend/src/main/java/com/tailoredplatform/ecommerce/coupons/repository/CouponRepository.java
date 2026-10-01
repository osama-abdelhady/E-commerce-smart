package com.tailoredplatform.ecommerce.coupons.repository;

import com.tailoredplatform.ecommerce.coupons.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);
}
