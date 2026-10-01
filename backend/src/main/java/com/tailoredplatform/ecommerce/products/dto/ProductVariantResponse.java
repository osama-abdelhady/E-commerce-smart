package com.tailoredplatform.ecommerce.products.dto;

import java.math.BigDecimal;

public record ProductVariantResponse(
        Long id,
        String sku,
        String size,
        String color,
        String colorHex,
        BigDecimal price,
        boolean inStock,
        Integer availableQuantity
) {
}
