package com.tailoredplatform.ecommerce.coupons.controller;

import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import com.tailoredplatform.ecommerce.coupons.dto.CouponRequest;
import com.tailoredplatform.ecommerce.coupons.dto.CouponResponse;
import com.tailoredplatform.ecommerce.coupons.entity.Coupon;
import com.tailoredplatform.ecommerce.coupons.repository.CouponRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Coupons", description = "Discount coupon management")
public class AdminCouponController {

    private final CouponRepository couponRepository;

    @GetMapping
    @Operation(summary = "List all coupons")
    public List<CouponResponse> list() {
        return couponRepository.findAll().stream().map(this::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a coupon")
    public CouponResponse create(@Valid @RequestBody CouponRequest request) {
        if (couponRepository.findByCode(request.code().toUpperCase()).isPresent()) {
            throw new BusinessRuleViolationException("A coupon with code " + request.code() + " already exists.");
        }
        Coupon coupon = new Coupon();
        applyFields(coupon, request);
        return toResponse(couponRepository.save(coupon));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a coupon")
    public CouponResponse update(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Coupon", id));
        applyFields(coupon, request);
        return toResponse(couponRepository.save(coupon));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a coupon")
    public void deactivate(@PathVariable Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Coupon", id));
        coupon.setActive(false);
        couponRepository.save(coupon);
    }

    private void applyFields(Coupon coupon, CouponRequest request) {
        coupon.setCode(request.code().toUpperCase());
        coupon.setDiscountType(Coupon.DiscountType.valueOf(request.discountType().toUpperCase()));
        coupon.setDiscountValue(request.discountValue());
        coupon.setMinimumOrderAmount(request.minimumOrderAmount() != null ? request.minimumOrderAmount() : BigDecimal.ZERO);
        coupon.setMaxRedemptions(request.maxRedemptions());
        coupon.setMaxRedemptionsPerUser(request.maxRedemptionsPerUser());
        coupon.setValidFrom(request.validFrom());
        coupon.setValidUntil(request.validUntil());
        coupon.setActive(request.isActive());
    }

    private CouponResponse toResponse(Coupon c) {
        return new CouponResponse(
                c.getId(), c.getCode(), c.getDiscountType().name(), c.getDiscountValue(),
                c.getMinimumOrderAmount(), c.getMaxRedemptions(), c.getMaxRedemptionsPerUser(),
                c.getRedemptionsCount(), c.getValidFrom(), c.getValidUntil(), c.isActive()
        );
    }
}
