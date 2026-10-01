package com.tailoredplatform.ecommerce.products.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailResponse(
        Long id,
        String sku,
        String slug,
        String name,
        String description,
        BigDecimal price,
        BigDecimal discountPrice,
        String currency,
        String brandName,
        String categoryName,
        String categorySlug,
        String status,
        BigDecimal averageRating,
        Integer reviewCount,
        boolean isBestSeller,
        boolean isNewArrival,
        List<ProductImageResponse> images,
        List<ProductVariantResponse> variants,
        List<String> availableSizes,
        List<String> availableColors
) {
}
