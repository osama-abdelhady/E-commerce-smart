package com.tailoredplatform.ecommerce.products.mapper;

import com.tailoredplatform.ecommerce.inventory.entity.Inventory;
import com.tailoredplatform.ecommerce.inventory.repository.InventoryRepository;
import com.tailoredplatform.ecommerce.products.dto.ProductDetailResponse;
import com.tailoredplatform.ecommerce.products.dto.ProductImageResponse;
import com.tailoredplatform.ecommerce.products.dto.ProductSummaryResponse;
import com.tailoredplatform.ecommerce.products.dto.ProductVariantResponse;
import com.tailoredplatform.ecommerce.products.entity.Product;
import com.tailoredplatform.ecommerce.products.entity.ProductVariant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A plain @Component rather than a MapStruct @Mapper: the stock-related
 * fields (inStock, availableQuantity) need Inventory data alongside the
 * Product entity, which doesn't fit MapStruct's pure entity->DTO mapping.
 *
 * Performance note (Phase 9 fix — previously flagged as a known N+1 in
 * Phases 4 and 8): toSummaryList()/toDetail() batch-fetch every variant's
 * Inventory row in ONE query via findByProductVariantIdIn, instead of the
 * original one-query-per-variant pattern. A 24-item listing page with, say,
 * 3 variants each now issues 1 inventory query instead of up to 72.
 */
@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final InventoryRepository inventoryRepository;

    /** Preferred entry point for listings — batches inventory lookups across every product in the page. */
    public List<ProductSummaryResponse> toSummaryList(List<Product> products) {
        Map<Long, Inventory> inventoryByVariantId = loadInventoryFor(products);
        return products.stream().map(p -> toSummary(p, inventoryByVariantId)).toList();
    }

    /** Single-product convenience overload — still O(1) queries (one inventory batch for this product's own variants). */
    public ProductSummaryResponse toSummary(Product product) {
        return toSummary(product, loadInventoryFor(List.of(product)));
    }

    public ProductDetailResponse toDetail(Product product) {
        Map<Long, Inventory> inventoryByVariantId = loadInventoryFor(List.of(product));

        List<ProductImageResponse> images = product.getImages().stream()
                .map(img -> new ProductImageResponse(img.getId(), img.getUrl(), img.getAltText(), img.getDisplayOrder()))
                .toList();

        List<ProductVariantResponse> variants = product.getVariants().stream()
                .map(v -> toVariantResponse(v, inventoryByVariantId))
                .toList();

        List<String> sizes = product.getVariants().stream()
                .map(ProductVariant::getSize)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream().toList();

        List<String> colors = product.getVariants().stream()
                .map(ProductVariant::getColor)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream().toList();

        return new ProductDetailResponse(
                product.getId(),
                product.getSku(),
                product.getSlug(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.getCurrency(),
                product.getBrand() != null ? product.getBrand().getName() : null,
                product.getCategory().getName(),
                product.getCategory().getSlug(),
                product.getStatus().name(),
                product.getAverageRating(),
                product.getReviewCount(),
                product.isBestSeller(),
                product.isNewArrival(),
                images,
                variants,
                sizes,
                colors
        );
    }

    private ProductSummaryResponse toSummary(Product product, Map<Long, Inventory> inventoryByVariantId) {
        String primaryImage = product.getImages().stream()
                .findFirst()
                .map(img -> img.getUrl())
                .orElse(null);

        boolean inStock = product.getVariants().stream()
                .anyMatch(v -> sellable(v, inventoryByVariantId) > 0);

        return new ProductSummaryResponse(
                product.getId(),
                product.getSku(),
                product.getSlug(),
                product.getName(),
                product.getBrand() != null ? product.getBrand().getName() : null,
                product.getCategory().getName(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.getCurrency(),
                primaryImage,
                product.getAverageRating(),
                product.getReviewCount(),
                product.isBestSeller(),
                product.isNewArrival(),
                inStock
        );
    }

    private ProductVariantResponse toVariantResponse(ProductVariant variant, Map<Long, Inventory> inventoryByVariantId) {
        int available = sellable(variant, inventoryByVariantId);
        return new ProductVariantResponse(
                variant.getId(),
                variant.getSku(),
                variant.getSize(),
                variant.getColor(),
                variant.getColorHex(),
                variant.effectivePrice(),
                available > 0,
                available
        );
    }

    private int sellable(ProductVariant variant, Map<Long, Inventory> inventoryByVariantId) {
        Inventory inv = inventoryByVariantId.get(variant.getId());
        return inv == null ? 0 : inv.getQuantityAvailable() - inv.getQuantityReserved();
    }

    private Map<Long, Inventory> loadInventoryFor(List<Product> products) {
        List<Long> variantIds = products.stream()
                .flatMap(p -> p.getVariants().stream())
                .map(ProductVariant::getId)
                .toList();
        if (variantIds.isEmpty()) return Map.of();
        return inventoryRepository.findByProductVariantIdIn(variantIds).stream()
                .collect(Collectors.toMap(inv -> inv.getProductVariant().getId(), Function.identity()));
    }
}
