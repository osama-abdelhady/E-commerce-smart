package com.tailoredplatform.ecommerce.shipping.dto;

import java.time.Instant;

public record ShipmentResponse(
        String carrier,
        String trackingNumber,
        String status,
        Instant shippedAt,
        Instant deliveredAt,
        Instant estimatedDeliveryAt
) {
}
