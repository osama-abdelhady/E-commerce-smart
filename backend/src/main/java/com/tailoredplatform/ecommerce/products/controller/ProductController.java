package com.tailoredplatform.ecommerce.products.controller;

import com.tailoredplatform.ecommerce.common.PageResponse;
import com.tailoredplatform.ecommerce.products.dto.ProductDetailResponse;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog: listing, filtering, search, and detail")
public class ProductController {

    private final ProductQueryService productQueryService;

    @GetMapping
    @Operation(summary = "List/filter products",
            description = "Supports category, brand, size, color, price range, rating, in-stock, keyword search, sorting, and pagination.")
    public PageResponse<ProductSummaryResponse> list(
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) List<String> brand,
            @RequestParam(required = false) List<String> size,
            @RequestParam(required = false) List<String> color,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Boolean inStockOnly,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "NEWEST") ProductSortOption sortBy,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "24") int pageSize
    ) {
        ProductFilter filter = new ProductFilter(
                categorySlug, brand, size, color, minPrice, maxPrice, minRating,
                inStockOnly, search, sortBy, page, pageSize
        );
        return productQueryService.search(filter);
    }

    @GetMapping("/search")
    @Operation(summary = "Keyword search — same filtering engine as list(), with `search` required")
    public PageResponse<ProductSummaryResponse> search(
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "24") int pageSize
    ) {
        ProductFilter filter = new ProductFilter(
                null, null, null, null, null, null, null, null, q,
                ProductSortOption.NEWEST, page, pageSize
        );
        return productQueryService.search(filter);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product detail by numeric id")
    public ProductDetailResponse getById(@PathVariable Long id) {
        return productQueryService.getById(id);
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get product detail by URL slug")
    public ProductDetailResponse getBySlug(@PathVariable String slug) {
        return productQueryService.getBySlug(slug);
    }

    @GetMapping("/{id}/related")
    @Operation(summary = "Related products — same category, capped at 8")
    public List<ProductSummaryResponse> related(@PathVariable Long id) {
        return productQueryService.getRelated(id);
    }

    @GetMapping("/best-sellers")
    public List<ProductSummaryResponse> bestSellers(@RequestParam(defaultValue = "8") int limit) {
        return productQueryService.getBestSellers(limit);
    }

    @GetMapping("/new-arrivals")
    public List<ProductSummaryResponse> newArrivals(@RequestParam(defaultValue = "8") int limit) {
        return productQueryService.getNewArrivals(limit);
    }
}
