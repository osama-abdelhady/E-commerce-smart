package com.tailoredplatform.ecommerce.categories.dto;

import jakarta.validation.constraints.NotBlank;

public record BrandRequest(
        @NotBlank String slug,
        @NotBlank String name,
        String logoUrl,
        boolean isActive
) {
}
