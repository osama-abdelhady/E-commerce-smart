package com.tailoredplatform.ecommerce.products.dto;

import java.math.BigDecimal;

public record ProductSummaryResponse(
        Long id,
        String sku,
        String slug,
        String name,
        String brandName,
        String categoryName,
        BigDecimal price,
        BigDecimal discountPrice,
        String currency,
        String primaryImageUrl,
        BigDecimal averageRating,
        Integer reviewCount,
        boolean isBestSeller,
        boolean isNewArrival,
        boolean inStock
) {
}
