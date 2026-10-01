package com.tailoredplatform.ecommerce.categories.dto;

public record BrandResponse(
        Long id,
        String slug,
        String name,
        String logoUrl
) {
}
