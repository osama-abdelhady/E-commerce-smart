package com.tailoredplatform.ecommerce.products.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        @DecimalMin("0.0") BigDecimal discountPrice,
        @NotNull Long categoryId,
        Long brandId,
        @NotBlank String status,
        boolean isBestSeller,
        boolean isNewArrival
) {
}
