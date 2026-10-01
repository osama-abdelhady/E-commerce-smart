package com.tailoredplatform.ecommerce.reviews.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotNull Long orderItemId,
        @Min(1) @Max(5) int rating,
        @Size(max = 150) String title,
        @Size(max = 2000) String body
) {
}
