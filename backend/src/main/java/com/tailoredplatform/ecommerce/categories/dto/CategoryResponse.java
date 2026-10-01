package com.tailoredplatform.ecommerce.categories.dto;

import java.util.List;

public record CategoryResponse(
        Long id,
        String slug,
        String name,
        String description,
        String imageUrl,
        Long parentId,
        List<CategoryResponse> children
) {
}
