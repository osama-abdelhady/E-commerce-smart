package com.tailoredplatform.ecommerce.orders.dto;

import java.time.Instant;

public record OrderTimelineEntryResponse(
        String status,
        String label,
        Instant timestamp,
        boolean completed
) {
}
