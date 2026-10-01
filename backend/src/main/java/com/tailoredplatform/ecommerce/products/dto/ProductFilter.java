package com.tailoredplatform.ecommerce.products.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Built by ProductController from individual @RequestParam values (so the
 * REST query string stays flat: ?categorySlug=business&brand=hugo-boss&
 * minPrice=100&sortBy=PRICE_ASC) and passed into ProductQueryService, which
 * turns it into a JPA Specification. Every field is optional/nullable —
 * an absent filter is simply not applied.
 */
public record ProductFilter(
        String categorySlug,
        List<String> brandSlugs,
        List<String> sizes,
        List<String> colors,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minRating,
        Boolean inStockOnly,
        String search,
        ProductSortOption sortBy,
        int page,
        int pageSize
) {
    public ProductFilter {
        if (page < 0) page = 0;
        if (pageSize <= 0 || pageSize > 100) pageSize = 24;
        if (sortBy == null) sortBy = ProductSortOption.NEWEST;
    }
}
