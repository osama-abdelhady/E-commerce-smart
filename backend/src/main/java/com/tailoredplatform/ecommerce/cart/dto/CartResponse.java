package com.tailoredplatform.ecommerce.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long cartId,
        List<CartItemResponse> items,
        int itemCount,
        BigDecimal subtotal
) {
}
