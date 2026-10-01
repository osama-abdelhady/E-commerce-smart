package com.tailoredplatform.ecommerce.cart.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productVariantId,
        String productName,
        String productSlug,
        String imageUrl,
        String size,
        String color,
        BigDecimal unitPrice,
        boolean priceChanged,
        int quantity,
        BigDecimal lineTotal,
        boolean inStock,
        Integer availableQuantity
) {
}
