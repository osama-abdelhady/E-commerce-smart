package com.tailoredplatform.ecommerce.users.dto;

import java.time.Instant;

public record CustomerSummaryResponse(
        Long id,
        String email,
        String fullName,
        String status,
        Instant createdAt,
        long orderCount
) {
}
