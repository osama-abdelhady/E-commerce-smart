package com.tailoredplatform.ecommerce.categories.controller;

import com.tailoredplatform.ecommerce.categories.dto.BrandResponse;
import com.tailoredplatform.ecommerce.categories.dto.CategoryResponse;
import com.tailoredplatform.ecommerce.categories.service.CategoryService;
import com.tailoredplatform.ecommerce.common.PageResponse;
import com.tailoredplatform.ecommerce.products.dto.ProductFilter;
import com.tailoredplatform.ecommerce.products.dto.ProductSortOption;
import com.tailoredplatform.ecommerce.products.dto.ProductSummaryResponse;
import com.tailoredplatform.ecommerce.products.service.ProductQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category tree, category-scoped product listing, and brands")
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductQueryService productQueryService;

    @GetMapping
    @Operation(summary = "Top-level categories with nested children")
    public List<CategoryResponse> list() {
        return categoryService.getCategoryTree();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Category detail by slug")
    public CategoryResponse getBySlug(@PathVariable String slug) {
        return categoryService.getBySlug(slug);
    }

    @GetMapping("/{slug}/products")
    @Operation(summary = "Products in this category — same filter/sort/pagination params as /products")
    public PageResponse<ProductSummaryResponse> products(
            @PathVariable String slug,
            @RequestParam(required = false) List<String> brand,
            @RequestParam(required = false, defaultValue = "NEWEST") ProductSortOption sortBy,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "24") int pageSize
    ) {
        ProductFilter filter = new ProductFilter(
                slug, brand, null, null, null, null, null, null, null, sortBy, page, pageSize
        );
        return productQueryService.search(filter);
    }

    @GetMapping("/brands/all")
    @Operation(summary = "All active brands, for the filter sidebar")
    public List<BrandResponse> brands() {
        return categoryService.getAllBrands();
    }
}
