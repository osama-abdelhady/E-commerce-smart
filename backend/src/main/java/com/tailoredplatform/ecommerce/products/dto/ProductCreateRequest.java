package com.tailoredplatform.ecommerce.products.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProductCreateRequest(
        @NotBlank @Size(max = 60) String sku,
        @NotBlank @Size(max = 200) String slug,
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        @DecimalMin("0.0") BigDecimal discountPrice,
        @NotNull Long categoryId,
        Long brandId,
        boolean isBestSeller,
        boolean isNewArrival,
        @Valid List<ImageInput> images,
        @Valid List<VariantInput> variants
) {
    public record ImageInput(@NotBlank String url, String altText, int displayOrder) {}

    public record VariantInput(
            @NotBlank String sku,
            String size,
            String color,
            String colorHex,
            BigDecimal priceOverride,
            @NotNull Integer initialQuantity
    ) {}
}
