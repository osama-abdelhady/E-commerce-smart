package com.tailoredplatform.ecommerce.products.specification;

import com.tailoredplatform.ecommerce.inventory.entity.Inventory;
import com.tailoredplatform.ecommerce.products.dto.ProductFilter;
import com.tailoredplatform.ecommerce.products.entity.Product;
import com.tailoredplatform.ecommerce.products.entity.ProductVariant;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Every customer-facing product query goes through here. Price filtering
 * and sorting use COALESCE(discount_price, price) — the same "effective
 * price" the storefront displays — rather than the base price alone, so a
 * discounted product filed under $400 actually shows up in a "$0-$400" filter.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> fromFilter(ProductFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only ever show active products to customers.
            predicates.add(cb.equal(root.get("status"), Product.Status.ACTIVE));

            if (filter.categorySlug() != null && !filter.categorySlug().isBlank()) {
                predicates.add(cb.equal(root.get("category").get("slug"), filter.categorySlug()));
            }

            if (filter.brandSlugs() != null && !filter.brandSlugs().isEmpty()) {
                predicates.add(root.get("brand").get("slug").in(filter.brandSlugs()));
            }

            var effectivePrice = cb.coalesce(root.get("discountPrice"), root.get("price"));

            if (filter.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(effectivePrice, filter.minPrice()));
            }
            if (filter.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(effectivePrice, filter.maxPrice()));
            }

            if (filter.minRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"), filter.minRating()));
            }

            if (filter.search() != null && !filter.search().isBlank()) {
                String pattern = "%" + filter.search().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("sku")), pattern)
                ));
            }

            boolean needsVariantJoin = hasSizeOrColorFilter(filter);
            if (needsVariantJoin) {
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.INNER);
                if (filter.sizes() != null && !filter.sizes().isEmpty()) {
                    predicates.add(variantJoin.get("size").in(filter.sizes()));
                }
                if (filter.colors() != null && !filter.colors().isEmpty()) {
                    predicates.add(variantJoin.get("color").in(filter.colors()));
                }
                query.distinct(true);
            }

            if (Boolean.TRUE.equals(filter.inStockOnly())) {
                predicates.add(cb.exists(inStockSubquery(root, query, cb)));
            }

            // Sorting is applied here (rather than via Pageable.getSort()) so
            // PRICE_ASC/PRICE_DESC can order by the same COALESCE expression
            // used for filtering — Pageable's Sort can only reference a plain
            // entity attribute path, not a computed expression.
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                switch (filter.sortBy()) {
                    case PRICE_ASC -> query.orderBy(cb.asc(effectivePrice));
                    case PRICE_DESC -> query.orderBy(cb.desc(effectivePrice));
                    case RATING -> query.orderBy(cb.desc(root.get("averageRating")));
                    case POPULARITY -> query.orderBy(cb.desc(root.get("reviewCount")));
                    case NEWEST -> query.orderBy(cb.desc(root.get("createdAt")));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasSizeOrColorFilter(ProductFilter filter) {
        return (filter.sizes() != null && !filter.sizes().isEmpty())
                || (filter.colors() != null && !filter.colors().isEmpty());
    }

    /** EXISTS (SELECT 1 FROM inventory WHERE inventory.variant.product = :product AND sellable > 0) */
    private static Subquery<Long> inStockSubquery(Root<Product> productRoot, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<Inventory> inventoryRoot = subquery.from(Inventory.class);
        Join<Inventory, ProductVariant> variantJoin = inventoryRoot.join("productVariant", JoinType.INNER);
        subquery.select(inventoryRoot.get("id"));
        subquery.where(cb.and(
                cb.equal(variantJoin.get("product"), productRoot),
                cb.greaterThan(
                        cb.diff(inventoryRoot.get("quantityAvailable"), inventoryRoot.get("quantityReserved")),
                        0)
        ));
        return subquery;
    }
}
