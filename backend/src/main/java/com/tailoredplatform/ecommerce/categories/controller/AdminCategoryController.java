package com.tailoredplatform.ecommerce.categories.controller;

import com.tailoredplatform.ecommerce.categories.dto.BrandRequest;
import com.tailoredplatform.ecommerce.categories.dto.BrandResponse;
import com.tailoredplatform.ecommerce.categories.dto.CategoryRequest;
import com.tailoredplatform.ecommerce.categories.dto.CategoryResponse;
import com.tailoredplatform.ecommerce.categories.entity.Brand;
import com.tailoredplatform.ecommerce.categories.entity.Category;
import com.tailoredplatform.ecommerce.categories.mapper.CategoryMapper;
import com.tailoredplatform.ecommerce.categories.repository.BrandRepository;
import com.tailoredplatform.ecommerce.categories.repository.CategoryRepository;
import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Categories & Brands", description = "Category and brand management")
public class AdminCategoryController {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final CategoryMapper categoryMapper;

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category")
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        Category category = new Category();
        applyCategoryFields(category, request);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update a category")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
        applyCategoryFields(category, request);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Deactivate a category")
    public void deactivateCategory(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
        category.setActive(false);
        categoryRepository.save(category);
    }

    @PostMapping("/brands")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a brand")
    public BrandResponse createBrand(@Valid @RequestBody BrandRequest request) {
        Brand brand = new Brand();
        applyBrandFields(brand, request);
        return categoryMapper.toResponse(brandRepository.save(brand));
    }

    @PutMapping("/brands/{id}")
    @Operation(summary = "Update a brand")
    public BrandResponse updateBrand(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Brand", id));
        applyBrandFields(brand, request);
        return categoryMapper.toResponse(brandRepository.save(brand));
    }

    @DeleteMapping("/brands/{id}")
    @Operation(summary = "Deactivate a brand")
    public void deactivateBrand(@PathVariable Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Brand", id));
        brand.setActive(false);
        brandRepository.save(brand);
    }

    private void applyCategoryFields(Category category, CategoryRequest request) {
        category.setSlug(request.slug());
        category.setName(request.name());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        category.setActive(request.isActive());
        category.setDisplayOrder(request.displayOrder());
        if (request.parentId() != null) {
            Category parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Category", request.parentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
    }

    private void applyBrandFields(Brand brand, BrandRequest request) {
        brand.setSlug(request.slug());
        brand.setName(request.name());
        brand.setLogoUrl(request.logoUrl());
        brand.setActive(request.isActive());
    }
}
