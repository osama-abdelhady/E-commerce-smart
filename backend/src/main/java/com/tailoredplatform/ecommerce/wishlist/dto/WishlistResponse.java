package com.tailoredplatform.ecommerce.wishlist.dto;

import java.util.List;

public record WishlistResponse(
        Long id,
        List<WishlistItemResponse> items
) {
}
