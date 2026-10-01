package com.tailoredplatform.ecommerce.categories.mapper;

import com.tailoredplatform.ecommerce.categories.dto.BrandResponse;
import com.tailoredplatform.ecommerce.categories.dto.CategoryResponse;
import com.tailoredplatform.ecommerce.categories.entity.Brand;
import com.tailoredplatform.ecommerce.categories.entity.Category;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    default CategoryResponse toResponse(Category category) {
        return toResponse(category, List.of());
    }

    default CategoryResponse toResponse(Category category, List<CategoryResponse> children) {
        if (category == null) return null;
        return new CategoryResponse(
                category.getId(),
                category.getSlug(),
                category.getName(),
                category.getDescription(),
                category.getImageUrl(),
                category.getParent() != null ? category.getParent().getId() : null,
                children
        );
    }

    default BrandResponse toResponse(Brand brand) {
        if (brand == null) return null;
        return new BrandResponse(brand.getId(), brand.getSlug(), brand.getName(), brand.getLogoUrl());
    }
}
