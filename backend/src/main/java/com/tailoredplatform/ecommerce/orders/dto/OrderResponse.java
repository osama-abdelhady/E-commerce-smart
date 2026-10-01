package com.tailoredplatform.ecommerce.orders.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        String status,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal shippingFee,
        BigDecimal taxTotal,
        BigDecimal grandTotal,
        String currency,
        Instant placedAt,
        boolean cancellable,
        List<OrderTimelineEntryResponse> timeline
) {
}
