package com.tailoredplatform.ecommerce.coupons.service;

import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.coupons.dto.CouponValidationResult;
import com.tailoredplatform.ecommerce.coupons.entity.Coupon;
import com.tailoredplatform.ecommerce.coupons.entity.CouponRedemption;
import com.tailoredplatform.ecommerce.coupons.repository.CouponRedemptionRepository;
import com.tailoredplatform.ecommerce.coupons.repository.CouponRepository;
import com.tailoredplatform.ecommerce.orders.entity.Order;
import com.tailoredplatform.ecommerce.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;

    /**
     * Called during checkout, inside OrderService's transaction, BEFORE the
     * order is persisted — validates the coupon is currently valid, the
     * order meets its minimum, and this user hasn't exceeded their per-user
     * redemption limit. Does not mutate anything; recordRedemption() below
     * does that once the order actually exists.
     */
    @Transactional(readOnly = true, propagation = Propagation.MANDATORY)
    public CouponValidationResult validate(String code, User user, BigDecimal orderSubtotal) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid coupon code."));

        if (!coupon.isCurrentlyValid()) {
            throw new BusinessRuleViolationException("This coupon is no longer valid.");
        }

        if (orderSubtotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new BusinessRuleViolationException(
                    "This coupon requires a minimum order of " + coupon.getMinimumOrderAmount() + ".");
        }

        long userRedemptions = couponRedemptionRepository.countByCouponIdAndUserId(coupon.getId(), user.getId());
        if (userRedemptions >= coupon.getMaxRedemptionsPerUser()) {
            throw new BusinessRuleViolationException("You have already used this coupon the maximum number of times.");
        }

        BigDecimal discount = coupon.calculateDiscount(orderSubtotal);
        return new CouponValidationResult(coupon, discount);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordRedemption(Coupon coupon, User user, Order order, BigDecimal discountApplied) {
        CouponRedemption redemption = new CouponRedemption();
        redemption.setCoupon(coupon);
        redemption.setUser(user);
        redemption.setOrder(order);
        redemption.setDiscountApplied(discountApplied);
        couponRedemptionRepository.save(redemption);

        coupon.setRedemptionsCount(coupon.getRedemptionsCount() + 1);
        couponRepository.save(coupon);
    }
}
