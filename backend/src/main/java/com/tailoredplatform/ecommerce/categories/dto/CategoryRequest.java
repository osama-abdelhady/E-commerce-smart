package com.tailoredplatform.ecommerce.categories.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank String slug,
        @NotBlank String name,
        String description,
        String imageUrl,
        Long parentId,
        boolean isActive,
        int displayOrder
) {
}
