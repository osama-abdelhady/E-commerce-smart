package com.tailoredplatform.ecommerce.orders.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        String productName,
        String sku,
        String variantLabel,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
