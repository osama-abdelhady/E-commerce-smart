package com.tailoredplatform.ecommerce.products.dto;

public record ProductImageResponse(
        Long id,
        String url,
        String altText,
        Integer displayOrder
) {
}
