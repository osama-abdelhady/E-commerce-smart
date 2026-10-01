package com.tailoredplatform.ecommerce.coupons;

import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.coupons.entity.Coupon;
import com.tailoredplatform.ecommerce.coupons.repository.CouponRedemptionRepository;
import com.tailoredplatform.ecommerce.coupons.repository.CouponRepository;
import com.tailoredplatform.ecommerce.coupons.service.CouponService;
import com.tailoredplatform.ecommerce.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock private CouponRepository couponRepository;
    @Mock private CouponRedemptionRepository couponRedemptionRepository;

    private CouponService couponService;
    private User user;

    @BeforeEach
    void setUp() {
        couponService = new CouponService(couponRepository, couponRedemptionRepository);
        user = new User();
        user.setId(1L);
    }

    private Coupon validCoupon() {
        Coupon coupon = new Coupon();
        coupon.setId(10L);
        coupon.setCode("SAVE10");
        coupon.setDiscountType(Coupon.DiscountType.PERCENTAGE);
        coupon.setDiscountValue(BigDecimal.TEN);
        coupon.setMinimumOrderAmount(BigDecimal.valueOf(50));
        coupon.setMaxRedemptionsPerUser(1);
        coupon.setValidFrom(Instant.now().minus(1, ChronoUnit.DAYS));
        coupon.setValidUntil(Instant.now().plus(30, ChronoUnit.DAYS));
        coupon.setActive(true);
        return coupon;
    }

    @Test
    void validate_succeeds_forAnEligibleOrder() {
        Coupon coupon = validCoupon();
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));
        when(couponRedemptionRepository.countByCouponIdAndUserId(10L, 1L)).thenReturn(0L);

        var result = couponService.validate("save10", user, BigDecimal.valueOf(100));

        assertThat(result.discountAmount()).isEqualByComparingTo("10.00"); // 10% of 100
    }

    @Test
    void validate_throws_forUnknownCode() {
        when(couponRepository.findByCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.validate("NOPE", user, BigDecimal.valueOf(100)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Invalid coupon");
    }

    @Test
    void validate_throws_forExpiredCoupon() {
        Coupon coupon = validCoupon();
        coupon.setValidUntil(Instant.now().minus(1, ChronoUnit.DAYS));
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.validate("SAVE10", user, BigDecimal.valueOf(100)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("no longer valid");
    }

    @Test
    void validate_throws_forInactiveCoupon() {
        Coupon coupon = validCoupon();
        coupon.setActive(false);
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.validate("SAVE10", user, BigDecimal.valueOf(100)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void validate_throws_whenOrderBelowMinimum() {
        Coupon coupon = validCoupon(); // minimum 50
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.validate("SAVE10", user, BigDecimal.valueOf(20)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("minimum order");
    }

    @Test
    void validate_throws_whenUserAlreadyAtPerUserRedemptionLimit() {
        Coupon coupon = validCoupon(); // maxRedemptionsPerUser = 1
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));
        when(couponRedemptionRepository.countByCouponIdAndUserId(10L, 1L)).thenReturn(1L);

        assertThatThrownBy(() -> couponService.validate("SAVE10", user, BigDecimal.valueOf(100)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("maximum number of times");
    }

    @Test
    void validate_throws_whenGlobalRedemptionCapReached() {
        Coupon coupon = validCoupon();
        coupon.setMaxRedemptions(5);
        coupon.setRedemptionsCount(5);
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.validate("SAVE10", user, BigDecimal.valueOf(100)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void fixedAmountDiscount_isCappedAtTheOrderSubtotal() {
        Coupon coupon = validCoupon();
        coupon.setDiscountType(Coupon.DiscountType.FIXED_AMOUNT);
        coupon.setDiscountValue(BigDecimal.valueOf(200)); // bigger than the order
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));
        when(couponRedemptionRepository.countByCouponIdAndUserId(10L, 1L)).thenReturn(0L);

        var result = couponService.validate("SAVE10", user, BigDecimal.valueOf(100));

        assertThat(result.discountAmount()).isEqualByComparingTo("100"); // never exceeds the subtotal
    }
}
