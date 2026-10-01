package com.tailoredplatform.ecommerce.inventory.dto;

public record InventoryResponse(
        Long id,
        Long productVariantId,
        String variantSku,
        String productName,
        Integer quantityAvailable,
        Integer quantityReserved,
        Integer sellable,
        Integer lowStockThreshold,
        boolean isLowStock
) {
}
