package com.tailoredplatform.ecommerce.reviews.dto;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long productId,
        String reviewerName,
        int rating,
        String title,
        String body,
        Instant createdAt
) {
}
