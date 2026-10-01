package com.tailoredplatform.ecommerce.wishlist.dto;

import java.math.BigDecimal;

public record WishlistItemResponse(
        Long id,
        Long productId,
        String productName,
        String productSlug,
        String imageUrl,
        BigDecimal price,
        BigDecimal discountPrice,
        boolean inStock
) {
}
